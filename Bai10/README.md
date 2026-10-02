# Chat Multicast Java Swing

## Yêu cầu
- Java 17+
- Java Swing
- TCP Server: cổng 5000
- UDP Multicast: nhóm chat dùng 239.1.1.x, cổng 8000+
- UDP Unicast: chat riêng giữa các client
- Server không có chức năng gửi tin nhắn.
- Server quản lý client/phòng và cấu hình TTL.
- Client luôn thuộc nhóm `SERVER` và không thể rời nhóm này.

## Chạy
Biên dịch:

```bash
javac -encoding UTF-8 *.java
```

Chạy server:

```bash
java ChatServer
```

Chạy client trên cùng máy:

```bash
java ChatClient
```

Máy khác:
- sửa `SERVER_HOST` trong `ChatClient.java` thành IP của máy server.
- mở TCP 5000.
- các client cần cho phép UDP trên các cổng 8000+ trong firewall.

## Kiến trúc
- TCP chỉ dùng cho đăng ký, danh sách online, tạo/giải tán/rời phòng, thêm thành viên và đồng bộ trạng thái.
- Group chat: client gửi DatagramPacket tới multicast group. Server cũng join group để giám sát. Mỗi client có số thứ tự riêng; khi phát hiện thiếu seq, client yêu cầu server điều khiển client gửi lại datagram.
- Private chat: client gửi UDP unicast trực tiếp tới IP/UDP port của client đích. Một bản sao metadata/nội dung được gửi TCP lên server để server ghi nhật ký phạm vi `PRIVATE`.
- Server không có nút/ô nhập tin nhắn và không phát sinh chat datagram; các thông báo rời/giải tán được client tự suy ra từ STATE.
- TTL do server đặt khi tạo phòng. TTL được gắn vào mỗi multicast socket trước khi gửi.

## Lưu ý
UDP multicast phụ thuộc vào mạng. Nếu chạy khác máy, router/switch/firewall phải cho phép multicast. Trong cùng LAN thường dễ kiểm tra hơn.
