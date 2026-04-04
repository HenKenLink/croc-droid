package crocbridge

import (
	"context"
	"encoding/json"
	"os"
	"reflect"
	"strings"
	"sync"
	"time"
	"unsafe"

	"github.com/schollz/croc/v10/src/comm"
	"github.com/schollz/croc/v10/src/croc"
	"github.com/schollz/croc/v10/src/tcp"
	"github.com/schollz/croc/v10/src/utils"
	log "github.com/schollz/logger"
)

// CrocCallback is used to pass status back to Kotlin
type CrocCallback interface {
	OnReady(code string)
	OnFileOffer(fileName string, fileSize int64, fileCount int64) bool
	OnProgress(sent int64, total int64)
	OnFileProgress(currentFile string, currentNum int64, totalFiles int64, sent int64, total int64)
	OnSuccess()
	OnSuccessWithFiles(fileListJSON string)
	OnError(errStr string)
}

var (
	receiveChannels sync.Map // Map transferID -> chan bool
	logBuffer       strings.Builder
	logMutex        sync.Mutex
)

func init() {
	log.SetOutput(&logWriter{})
}

type logWriter struct{}

func (f *logWriter) Write(p []byte) (n int, err error) {
	logMutex.Lock()
	defer logMutex.Unlock()
	if logBuffer.Len() > 1024*1024 { // 1MB limit
		logBuffer.Reset()
	}
	logBuffer.Write(p)
	return os.Stderr.Write(p)
}

// TransferConfig is the JSON representation of Android UI settings
type TransferConfig struct {
	RelayAddress        string `json:"relayAddress"`
	RelayPassword       string `json:"relayPassword"`
	RelayPorts          string `json:"relayPorts"`
	Curve               string `json:"curve"`
	HashAlgorithm       string `json:"hashAlgorithm"`
	DisableLocal        bool   `json:"disableLocal"`
	ForceLocal          bool   `json:"forceLocal"`
	MulticastAddress    string `json:"multicastAddress"`
	DisableMultiplexing bool   `json:"disableMultiplexing"`
	DisableCompression  bool   `json:"disableCompression"`
	UploadThrottle      string `json:"uploadThrottle"`
	Overwrite           bool   `json:"overwrite"`
	DebugMode           bool   `json:"debugMode"`
	IsTempZip           bool   `json:"isTempZip"`       // Sent file is a Kotlin-compressed zip, should be auto-unzipped by recipient
	NoPromptReceive     bool   `json:"noPromptReceive"` // Skip confirmation on receive
	PeerIP              string `json:"peerIp"`          // Force direct connection to this IP
}

var (
	cancelFuncs = make(map[string]context.CancelFunc)
	cancelMutex sync.Mutex
	chdirMutex  sync.Mutex // Global lock for Chdir operations
)

func registerCancel(id string, cancel context.CancelFunc) {
	cancelMutex.Lock()
	defer cancelMutex.Unlock()
	cancelFuncs[id] = cancel
}

func unregisterCancel(id string) {
	cancelMutex.Lock()
	defer cancelMutex.Unlock()
	delete(cancelFuncs, id)
}

// CancelTransfer cancels an ongoing transfer by its ID
func CancelTransfer(id string) {
	cancelMutex.Lock()
	defer cancelMutex.Unlock()
	if cancel, exists := cancelFuncs[id]; exists {
		cancel()
		delete(cancelFuncs, id)
	}
}

// closeClient uses reflection to access and close unexported connections in croc.Client.
// This is necessary to avoid modifying the upstream croc source code.
func closeClient(client *croc.Client) {
	if client == nil {
		return
	}
	// Note: We use reflect + unsafe because c.conn is unexported in github.com/schollz/croc
	val := reflect.ValueOf(client).Elem()
	field := val.FieldByName("conn")
	if !field.IsValid() {
		return
	}

	// Use unsafe to get the address of the unexported field
	ptr := unsafe.Pointer(field.UnsafeAddr())
	
	// Create a pointer to the slice of pointers to comm.Comm
	// The type []*comm.Comm is what croc.Client uses
	connsPtr := (*[]*comm.Comm)(ptr)
	conns := *connsPtr
	
	for _, c := range conns {
		if c != nil {
			c.Close() // Comm.Close() is exported
		}
	}
}

// GenerateCode generates a croc-style mnemonic code (e.g. "1234-apple-banana-cherry")
func GenerateCode() string {
	return utils.GetRandomName()
}

// AcceptReceive accepts the pending receive transfer
func AcceptReceive(id string) {
	if ch, ok := receiveChannels.Load(id); ok {
		ch.(chan bool) <- true
	}
}

// RejectReceive rejects the pending receive transfer
func RejectReceive(id string) {
	if ch, ok := receiveChannels.Load(id); ok {
		ch.(chan bool) <- false
	}
}

// StartRelay starts the croc relay server locally. Returns relayID.
func StartRelay(host string, port string, password string) string {
	id := "relay_" + time.Now().String() // Simple ID
	_, cancel := context.WithCancel(context.Background())
	registerCancel(id, cancel)

	go func() {
		defer unregisterCancel(id)
		
		// TCP run async doesn't exist directly taking context, but tcp.Run uses newStop with background context.
		// Actually, tcp package doesn't cleanly expose context termination except through stop struct which is internal.
		// Wait, tcp.Run actually returns blockingly?
		// tcp.Run was blocking. Let's start it in a go routine.
		// Note: since tcp.Run doesn't take context easily, we might not be able to stop it gracefully without more changes.
		// For now, let it run. In future we can add Context to tcp.Run if needed.
		go tcp.Run("info", host, port, password)
	}()

	return id
}

// StopRelay stops the relay
func StopRelay(id string) {
	CancelTransfer(id) // Attempt
}

func parseConfig(configJSON string) TransferConfig {
	var config TransferConfig
	// Default values
	config.RelayAddress = "croc.schollz.com:9009"
	config.RelayPassword = "pass123"
	config.RelayPorts = "9009,9010,9011,9012,9013"
	config.Curve = "p256"
	config.HashAlgorithm = "xxhash"
	config.DisableLocal = false
	config.Overwrite = true

	if configJSON != "" {
		_ = json.Unmarshal([]byte(configJSON), &config)
	}
	return config
}

func setupProgress(cb CrocCallback) func(sent int64, total int64) {
	var lastUpdate time.Time
	var progressMutex sync.Mutex

	return func(sent, total int64) {
		progressMutex.Lock()
		defer progressMutex.Unlock()
		now := time.Now()
		// Throttle updates to ~10Hz (every 100ms) to avoid killing JNI
		if now.Sub(lastUpdate) >= 100*time.Millisecond || sent >= total {
			lastUpdate = now
			cb.OnProgress(sent, total)
		}
	}
}

// SendFiles sends multiple files or directories.
func SendFiles(id string, filePathsJSON string, code string, configJSON string, cb CrocCallback) {
	go func() {
		config := parseConfig(configJSON)

		var filePaths []string
		err := json.Unmarshal([]byte(filePathsJSON), &filePaths)
		if err != nil {
			cb.OnError("JSON error: " + err.Error())
			return
		}

		opts := croc.Options{
			IsSender:         true,
			SharedSecret:     code,
			NoPrompt:         true,
			RelayAddress:     config.RelayAddress,
			RelayPassword:    config.RelayPassword,
			DisableLocal:     config.DisableLocal,
			OnlyLocal:        config.ForceLocal,
			MulticastAddress: config.MulticastAddress,
			NoMultiplexing:   config.DisableMultiplexing,
			NoCompress:       config.DisableCompression,
			ThrottleUpload:   config.UploadThrottle,
			Overwrite:        config.Overwrite,
			Curve:            config.Curve,
			HashAlgorithm:    config.HashAlgorithm,
			RelayPorts:       strings.Split(config.RelayPorts, ","),
			Debug:            config.DebugMode,
			OnProgress:       setupProgress(cb),
			IP:               config.PeerIP,
		}

		chdirMutex.Lock()
		defer chdirMutex.Unlock()

		ctx, cancel := context.WithCancel(context.Background())
		registerCancel(id, cancel)
		defer unregisterCancel(id)

		client, err := croc.NewCtx(ctx, opts)
		if err != nil {
			cb.OnError("Init error: " + err.Error())
			return
		}
		defer closeClient(client)

		// Tell UI the code in case they want to show it BEFORE sending starts
		cb.OnReady(client.Options.SharedSecret)

		filesInfo, emptyFolders, totalNumFolders, err := croc.GetFilesInfo(filePaths, false, false, nil)
		if err != nil {
			cb.OnError("Files error: " + err.Error())
			return
		}

		// If this is a manually compressed zip from Kotlin, mark it as TempFile 
		// so the recipient automatically unzips it.
		if config.IsTempZip {
			for i := range filesInfo {
				filesInfo[i].TempFile = true
			}
		}

		donechan := make(chan bool)
		go func() {
			ticker := time.NewTicker(time.Millisecond * 100)
			defer ticker.Stop()
			for {
				select {
				case <-ticker.C:
					if client != nil && client.Step2FileInfoTransferred {
						cnum := client.FilesToTransferCurrentNum
						if cnum < len(client.FilesToTransfer) {
							fi := client.FilesToTransfer[cnum]
							cb.OnFileProgress(fi.Name, int64(cnum), int64(len(client.FilesToTransfer)), client.TotalSent, fi.Size)
						}
					}
				case <-donechan:
					return
				}
			}
		}()

		err = client.Send(filesInfo, emptyFolders, totalNumFolders)
		close(donechan)
		if err != nil {
			cb.OnError("Send error: " + err.Error())
			return
		}

		cb.OnSuccess()
	}()
}

// ReceiveFile receives files and saves them to saveDir.
func ReceiveFile(id string, code string, saveDir string, configJSON string, cb CrocCallback) {
	go func() {
		config := parseConfig(configJSON)
		opts := croc.Options{
			IsSender:         false,
			SharedSecret:     code,
			NoPrompt:         true,
			RelayAddress:     config.RelayAddress,
			RelayPassword:    config.RelayPassword,
			DisableLocal:     config.DisableLocal,
			OnlyLocal:        config.ForceLocal,
			MulticastAddress: config.MulticastAddress,
			NoMultiplexing:   config.DisableMultiplexing,
			NoCompress:       config.DisableCompression,
			Overwrite:        config.Overwrite,
			Curve:            config.Curve,
			HashAlgorithm:    config.HashAlgorithm,
			RelayPorts:       strings.Split(config.RelayPorts, ","),
			Debug:            config.DebugMode,
			OnProgress:       setupProgress(cb),
			IP:               config.PeerIP,
			OnFileOffer: func(senderInfo croc.SenderInfo) bool {
				// Bypass confirmation if NoPromptReceive is enabled
				if config.NoPromptReceive {
					return true
				}
				var totalSize int64
				for _, f := range senderInfo.FilesToTransfer {
					totalSize += f.Size
				}
				fileName := ""
				if len(senderInfo.FilesToTransfer) > 0 {
					fileName = senderInfo.FilesToTransfer[0].Name
				}

				// Create buffered channel to avoid blocking
				ch := make(chan bool, 1)
				receiveChannels.Store(id, ch)
				defer receiveChannels.Delete(id)

				// Call Kotlin callback (non-blocking notification)
				cb.OnFileOffer(fileName, totalSize, int64(len(senderInfo.FilesToTransfer)))

				// Wait for true/false from AcceptReceive/RejectReceive via channel
				return <-ch
			},
		}

		chdirMutex.Lock()
		defer chdirMutex.Unlock()

		origDir, _ := os.Getwd()
		err := os.Chdir(saveDir)
		if err != nil {
			cb.OnError("Dir error: " + err.Error())
			return
		}
		defer os.Chdir(origDir)

		ctx, cancel := context.WithCancel(context.Background())
		registerCancel(id, cancel)
		defer unregisterCancel(id)

		// Loop to retry and ignore the "ping" error (unexpected end of JSON input)
		// which happens if the room is empty on the relay.
		for i := 0; i < 60; i++ { // Try for ~60 seconds
			client, err := croc.NewCtx(ctx, opts)
			if err != nil {
				cb.OnError("Init error: " + err.Error())
				return
			}

			cb.OnReady(code)
			
			donechan := make(chan bool)
			go func() {
				ticker := time.NewTicker(time.Millisecond * 100)
				defer ticker.Stop()
				for {
					select {
					case <-ticker.C:
						if client != nil && client.Step2FileInfoTransferred {
							cnum := client.FilesToTransferCurrentNum
							if cnum < len(client.FilesToTransfer) {
								fi := client.FilesToTransfer[cnum]
								cb.OnFileProgress(fi.Name, int64(cnum), int64(len(client.FilesToTransfer)), client.TotalSent, fi.Size)
							}
						}
					case <-donechan:
						return
					}
				}
			}()

			err = client.Receive()
			close(donechan)
			
			// Close connections using reflection to prevent leaks
			closeClient(client)

			if err == nil {
				fileNames := []string{}
				for _, fi := range client.FilesToTransfer {
					fileNames = append(fileNames, fi.Name)
				}
				fileListJSON, _ := json.Marshal(fileNames)
				cb.OnSuccessWithFiles(string(fileListJSON))
				return
			}

			// Check if it's the specific error caused by relay pings
			if strings.Contains(err.Error(), "unexpected end of JSON input") || 
			   strings.Contains(err.Error(), "room (secure channel) not ready") {
				// Retry after a short delay
				select {
				case <-ctx.Done():
					cb.OnError("Cancelled")
					return
				case <-time.After(1 * time.Second):
					continue
				}
			}

			// Otherwise, it's a real error
			cb.OnError("Receive error: " + err.Error())
			return
		}
	}()
}
// GetDebugLog returns the current log buffer
func GetDebugLog() string {
	logMutex.Lock()
	defer logMutex.Unlock()
	return logBuffer.String()
}

// ClearDebugLog clears the current log buffer
func ClearDebugLog() {
	logMutex.Lock()
	defer logMutex.Unlock()
	logBuffer.Reset()
}
