package codeminders.yfrog.lib.network;

import java.util.*;
import java.io.*;
import net.rim.device.api.io.Base64OutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.rim.device.api.io.http.HttpProtocolConstants;

public class ContentProviderChunkedData implements ContentProvider {
    private int chunkSize;
    private String contentType;
    private byte[] data;
    private int offset;
    public ContentProviderChunkedData(String contentType, byte[] data, int chunkSize ) {
        this.contentType = contentType;
        this.data = data;
        this.chunkSize = chunkSize;
        offset = 0;
    }
    public String getContentType() {
        return contentType;
    }
    public int getContentLength() {
        return nextChunkLen();
    }
    public String getContentRange() {
        int toSendLen = nextChunkLen();
        String val = "bytes "+String.valueOf(offset)+"-"+String.valueOf(offset+toSendLen-1)+"/"+String.valueOf(data.length);
        return val;
    }
    public void writeContent(OutputStream os) throws IOException {
        os.write(data,offset,nextChunkLen());
        offset += chunkSize;
    }
    private int nextChunkLen() {
        int toSendLen = (data.length >= offset + chunkSize) ? chunkSize : data.length - offset;
        return toSendLen;
    }
}
