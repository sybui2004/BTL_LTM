# RoomScreenController Refactoring Guide

## Tổng quan

`RoomScreenController` đã được refactor từ 1 file lớn (964 dòng) thành nhiều helper classes nhỏ hơn, dễ maintain hơn.

## Cấu trúc mới

```
controller/
├── RoomScreenControllerRefactored.java (245 dòng) - Main orchestration
├── room/
│   ├── RoomStateManager.java (49 dòng) - Quản lý state
│   ├── RoomUIUpdater.java (94 dòng) - Update UI cho room
│   ├── RoomManager.java (168 dòng) - Quản lý room operations
│   ├── InviteItemBuilder.java (103 dòng) - Build invite UI
│   └── TCPMessageHandler.java (204 dòng) - Handle TCP messages
└── friend/
    ├── FriendListManager.java (165 dòng) - Quản lý friend list & tabs
    └── FriendItemBuilder.java (155 dòng) - Build friend UI items
```

## Chi tiết từng class

### 1. RoomScreenControllerRefactored
**Mục đích:** Main controller, orchestrate tất cả helper classes

**Responsibilities:**
- Khởi tạo FXML components
- Khởi tạo và wire các helper classes
- Setup UI (avatars, popup)
- Load avatars (utility method)

**Kích thước:** ~245 dòng (giảm 75% so với 964 dòng)

### 2. RoomStateManager
**Mục đích:** Quản lý state của room

**State:**
- `currentRoomId`: ID phòng hiện tại
- `currentGuestId`: ID guest trong phòng
- `currentHostId`: ID host (nếu user là guest)
- `isHost`: User hiện tại là host hay guest

**Methods:**
- Getters/setters cho tất cả states
- `isRoomFull()`: Check phòng đã đầy chưa

### 3. RoomUIUpdater
**Mục đích:** Update UI elements cho room

**Responsibilities:**
- `updateGuestInfo()`: Cập nhật avatar & tên guest
- `updateHostInfo()`: Cập nhật avatar & tên host
- `clearGuestInfo()`: Clear info khi guest rời
- `setPlayButtonEnabled()`: Enable/disable Play button

**Dependencies:**
- Nhận `ImageLoader` interface để load avatars
- Tránh circular dependency

### 4. RoomManager
**Mục đích:** Quản lý room operations

**Responsibilities:**
- `createRoom()`: Tạo phòng hoặc reuse existing
- `handleInviteUser()`: Gửi invite
- `loadInvites()`: Load pending invites
- `handleAcceptInvite()`: Chấp nhận invite
- `handleRejectInvite()`: Từ chối invite

**Dependencies:**
- `RoomStateManager`: Lấy room state
- `BiConsumer<String, Alert.AlertType>`: Hiển thị alerts

### 5. TCPMessageHandler
**Mục đích:** Handle tất cả TCP messages

**TCP Messages:**
- `USER_STATUS`: User online/offline
- `INVITE_RECEIVED`: Nhận invite mới
- `ROOM_UPDATED`: Guest join (cho host)
- `ROOM_JOINED`: Join as guest
- `GUEST_LEFT`: Guest rời phòng
- `HOST_PROMOTED`: Promoted lên host

**Dependencies:**
- `RoomUIUpdater`: Update UI
- `Runnable onRefreshTab`: Refresh tab hiện tại
- `Runnable onLoadInvites`: Reload invites
- `RoomStateManager`: Update state

### 6. FriendListManager
**Mục đích:** Quản lý friend list, tabs, search

**Responsibilities:**
- `setupTabs()`: Setup tab switching
- `switchTab()`: Switch giữa Friends/Strangers/Recent
- `refreshCurrentTab()`: Refresh tab hiện tại
- `handleSearch()`: Xử lý search
- `executeSearch()`: Thực hiện search

**Dependencies:**
- `FriendItemBuilder`: Build UI items
- FXML components (listContainer, tabs, etc.)

### 7. FriendItemBuilder
**Mục đích:** Build UI components cho friend/user items

**Responsibilities:**
- `createFriendItem(UserSummary)`: Tạo item từ UserSummary
- `createFriendItemFromDTO(FriendDTO)`: Tạo item từ FriendDTO
- Hide '+' button logic (offline, room full, etc.)

**Dependencies:**
- `RoomStateManager`: Check room state
- `ImageLoader`: Load avatars
- `Consumer<Long>`: Callback khi click invite

### 8. InviteItemBuilder
**Mục đích:** Build UI components cho invite items

**Responsibilities:**
- `createInviteItem(InviteDTO)`: Tạo invite UI
- Load sender avatar asynchronously
- Setup accept/reject buttons

**Dependencies:**
- `ImageLoader`: Load avatars
- `Consumer<InviteDTO>`: Callbacks cho accept/reject

## Migration Plan

### Step 1: Test hiện tại
```bash
# Backup controller hiện tại
mv RoomScreenController.java RoomScreenController.java.backup
```

### Step 2: Sử dụng refactored version
```bash
# Rename refactored version
mv RoomScreenControllerRefactored.java RoomScreenController.java
```

### Step 3: Update FXML
```xml
<!-- Không cần thay đổi, vì tên class vẫn là RoomScreenController -->
```

### Step 4: Test
1. ✅ Login thành công
2. ✅ Tabs switching
3. ✅ Search strangers
4. ✅ Friend list hiển thị
5. ✅ Invite friends
6. ✅ Accept/reject invites
7. ✅ TCP real-time updates
8. ✅ Host/guest promotion

## Benefits

### Trước khi refactor:
- ❌ 1 file 964 dòng
- ❌ Khó maintain
- ❌ Khó test
- ❌ Mixing concerns (UI, business logic, TCP)

### Sau khi refactor:
- ✅ 8 files, mỗi file ~50-200 dòng
- ✅ Single Responsibility Principle
- ✅ Dễ test từng component
- ✅ Separation of Concerns
- ✅ Reusable components
- ✅ Clear dependencies

## Testing

### Unit Test Example
```java
@Test
public void testRoomStateManager() {
    RoomStateManager manager = new RoomStateManager();
    manager.setCurrentGuestId(123L);
    assertTrue(manager.isRoomFull());
}
```

### Integration Test
```java
@Test
public void testRoomManager() {
    RoomStateManager state = new RoomStateManager();
    RoomManager manager = new RoomManager(state, (msg, type) -> {});
    manager.createRoom();
    // Assert room created
}
```

## Future Improvements

1. **Add interfaces** cho dependency injection
2. **Extract constants** (colors, sizes) vào separate class
3. **Add logging** framework
4. **Add error recovery** logic
5. **Add unit tests** cho mỗi component
6. **Extract styles** vào CSS

## Conclusion

Refactoring này giúp code:
- Dễ đọc hơn
- Dễ maintain hơn
- Dễ test hơn
- Dễ mở rộng hơn

**Recommend:** Sử dụng refactored version cho production! 🚀

