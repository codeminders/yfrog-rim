package codeminders.yfrog.lib.network;

import java.util.*;
import java.io.*;
import net.rim.device.api.io.Base64OutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.rim.device.api.io.http.HttpProtocolConstants;


public class ContentMultipartFormData implements ContentProvider {
    public String boundary;
    private MimePart[] parts;
    private boolean writeBinary;
    public ContentMultipartFormData(MimePart[] parts, boolean writeBinary) {
        this.parts = parts;
        this.writeBinary = writeBinary;
        Random rnd = new Random();
        boundary = "_=_Part_"+String.valueOf(rnd.nextInt())+"_"+String.valueOf(rnd.nextInt());
    }
    public String getContentType() {
        return "multipart/form-data; boundary="+boundary;
    }
    public int getContentLength() {
        int len = 0;
        ByteArrayOutputStream tmpOs = new ByteArrayOutputStream();
        try {
            writeContent(tmpOs);
            tmpOs.flush();
            byte[] data = tmpOs.toByteArray();
            len = data.length;
        }
        catch( IOException ie) {
        }
        finally {
            try{ tmpOs.close(); } catch( IOException ie) {}
        }
        return len;
    }
    public String getContentRange() {
        return null;
    }
    public void writeContent(OutputStream os) throws IOException {
        String s;
        for(int i=0; i<parts.length; i++) {
            MimePart part = parts[i];
            s = "--"+boundary+"\r\n";
            os.write(s.getBytes("UTF-8"));
            if( part.contentType!=null ) {
                s = "Content-Type: "+part.contentType+"\r\n";
                os.write(s.getBytes("UTF-8"));
            }
            if( part.contentDisposition!=null ) {
                s = "Content-Disposition: "+part.contentDisposition+"\r\n";
                os.write(s.getBytes("UTF-8"));
            }
            if( part.isBinary() && writeBinary) {
                //s = "Content-Transfer-Encoding: binary\r\n";
                s = "Content-Disposition: "+part.contentDisposition+"\r\n";
                os.write(s.getBytes("UTF-8"));
            }
            os.write("\r\n".getBytes());
            if( !part.isBinary() ) {
                // write text data
                s = part.textData;
                if (s != null)
                    os.write(s.getBytes("UTF-8"));
            }
            if( part.isBinary() && writeBinary ) {
                if (part.binData != null)
                    os.write(part.binData);
            }
            os.write("\r\n".getBytes());
        }
        s = "--"+boundary+"--\r\n";
        os.write(s.getBytes("UTF-8"));
    }
    public MimePart getFirstBinaryPart() {
        for(int i=0; i<parts.length; i++) {
            MimePart part = parts[i];
            if(part.isBinary())
                return part;
        }
        return null;
    }
    public static class MimePart {
        public String contentType;
        public String contentDisposition;
        public String textData;
        public byte[] binData;
        public MimePart(String contentType, String contentDisposition, String textData) {
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
            this.textData = textData;
        }
        public MimePart(String contentType, String contentDisposition, byte[] binData) {
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
            this.binData = binData;
        }
        public void write(OutputStream os, String boundary) throws IOException {

        }
        public int getContentLength() {
            if( binData!=null )
                return binData.length;
            else
                return textData.length();
        }
        public boolean isBinary() {
            if( binData!=null )
                return true;
            return false;
        }
    }

}
