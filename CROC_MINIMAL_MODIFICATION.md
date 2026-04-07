# Minimal Croc Modification for File Confirmation

## Problem
File confirmation cannot be implemented purely in the bridge because:
- `client.Receive()` starts immediately and doesn't wait
- By the time we detect `Step2FileInfoTransferred` via polling, data transfer has already begun
- Cannot block the transfer retroactively

## Solution
Add **ONE callback** to croc: `OnFileOffer`

This is the minimal modification needed to support Android UI confirmation.

## Modification Details

### 1. Add OnFileOffer to Options (1 line)
**File**: `external/croc/src/croc/croc.go`
**Location**: Options struct

```go
type Options struct {
    // ... existing fields ...
    OnFileOffer func(senderInfo SenderInfo) bool  // ← ADD THIS
}
```

### 2. Call OnFileOffer in processMessageFileInfo
**File**: `external/croc/src/croc/croc.go`
**Location**: `processMessageFileInfo()` function

```go
// BEFORE (original):
if !c.Options.NoPrompt || c.Options.Ask || senderInfo.Ask {
    // CLI prompt logic
}

// AFTER (modified):
if c.Options.OnFileOffer != nil {
    if !c.Options.OnFileOffer(senderInfo) {
        err = message.Send(c.conn[0], c.Key, message.Message{
            Type:    message.TypeError,
            Message: "refusing files",
        })
        if err != nil {
            return false, err
        }
        return true, fmt.Errorf("refused files")
    }
} else if !c.Options.NoPrompt || c.Options.Ask || senderInfo.Ask {
    // CLI prompt logic (fallback)
}
```

## Why This Works

1. **Blocking**: `OnFileOffer` is called INSIDE `Receive()` BEFORE transfer starts
2. **Synchronous**: The callback blocks until it returns true/false
3. **Early**: Called at the right moment - after file info, before data transfer
4. **Clean**: Doesn't break existing CLI behavior (uses fallback)

## Bridge Implementation

```go
OnFileOffer: func(senderInfo croc.SenderInfo) bool {
    if config.NoPromptReceive {
        return true  // Auto-accept
    }
    
    // Calculate file info
    var totalSize int64
    for _, f := range senderInfo.FilesToTransfer {
        totalSize += f.Size
    }
    fileName := senderInfo.FilesToTransfer[0].Name
    
    // Create channel for user response
    ch := make(chan bool, 1)
    receiveChannels.Store(id, ch)
    defer receiveChannels.Delete(id)
    
    // Notify Kotlin UI (non-blocking)
    cb.OnFileOffer(fileName, totalSize, int64(len(senderInfo.FilesToTransfer)))
    
    // BLOCK here until user accepts/rejects
    return <-ch
}
```

## Progress Tracking (No Modification)

Progress is handled purely in bridge via polling - no croc modification needed:

```go
go func() {
    ticker := time.NewTicker(time.Millisecond * 100)
    for {
        select {
        case <-ticker.C:
            if client != nil && client.Step2FileInfoTransferred {
                // Report progress using public fields
                cb.OnFileProgress(...)
            }
        }
    }
}()
```

## Maintenance

When updating croc upstream:

1. **Create patch file** (one time):
   ```bash
   cd external/croc
   git diff src/croc/croc.go > ../../croc-onfileoffer.patch
   ```

2. **Update croc**:
   ```bash
   cd external/croc
   git pull upstream main
   ```

3. **Re-apply patch**:
   ```bash
   git apply ../../croc-onfileoffer.patch
   ```

4. **Test**: Verify file confirmation dialog appears and blocks transfer

## Summary

- **Croc modification**: 2 locations, ~15 lines total
- **Reason**: Only way to block transfer before it starts
- **Alternative**: None that works correctly
- **Maintenance**: Simple patch file
- **Benefit**: Proper file confirmation that actually blocks
