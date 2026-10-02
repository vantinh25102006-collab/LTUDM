# MulticastChatJava

## Kiến trúc
- TCP 5000: kênh điều khiển giữa client và server.
- UDP Multicast: chat nhóm, địa chỉ lớp D `239.x.x.x`.
- UDP Unicast: chat riêng client -> client.
- UDP Broadcast: server gửi tin vào `SERVER` theo địa chỉ đích nhập trên giao diện, cổng `5005`.

## UDP Server Broadcast
Trên giao diện Server có:
- Ô `UDP đích`: ví dụ `192.168.1.255` hoặc `255.255.255.255`.
- Ô nhập nội dung.
- Nút `Gửi UDP`.

Server tạo gói `ChatMessage` với:
- scope = `SERVER`
- groupId = `SERVER`
- senderName = `SERVER`
- thời gian gửi đầy đủ `dd/MM/yyyy HH:mm:ss`

Client mở thêm UDP listener tại cổng `5005`. Khi nhận đúng gói `scope=SERVER` và `groupId=SERVER`, client đưa tin vào lịch sử chat `SERVER`.

Lưu ý: `192.168.1.255` là logical broadcast cho subnet /24 tương ứng. `255.255.255.255` là limited/physical broadcast; chỉ dùng khi muốn kiểm thử phạm vi đó. Nếu bài yêu cầu loại bỏ physical broadcast thì chỉ nhập broadcast logic của subnet.

## Hiển thị
Tên người gửi trong khung chat được in đậm; thời gian và nội dung vẫn hiển thị đầy đủ.

## Chạy
```bash
javac -encoding UTF-8 *.java
java ChatServer
java ChatClient
```
Khi chạy khác máy, sửa `SERVER_HOST` trong `ChatClient.java` thành IP LAN của máy Server.
