package utils;

public class TransferInfo {
    private final String uuid;
    private final String sender;
    private final String fileName;
    private final String fileSize;

    public TransferInfo(String uuid, String sender, String fileName, String fileSize) {
        this.uuid = uuid;
        this.sender = sender;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSender() {
        return sender;
    }

    public String getFileName() {
        return fileName;
    }


    @Override
    public String toString() {
        return "TransferObject{" +
                "uuid='" + uuid + '\'' +
                ", sender='" + sender + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize='" + fileSize + '\'' +
                '}';
    }
}
