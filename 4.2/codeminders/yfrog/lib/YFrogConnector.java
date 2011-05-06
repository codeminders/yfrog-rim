package codeminders.yfrog.lib;

import java.lang.*;
import java.io.InputStream;
import java.util.*;
import net.rim.device.api.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import codeminders.yfrog.lib.items.*;
import codeminders.yfrog.lib.network.*;
import net.rim.device.api.xml.parsers.SAXParserFactory;

/**
 * YFrogConnector is main interface class to YFrog image and video services.
 * Class defines constants and provides appropriate methods.
 * see <a href="http://code.google.com/p/imageshackapi/wiki/YFrogAPI"> for service description
 */
public class YFrogConnector {

    /** Image conversion templates types supported by YFrog server */
    public static final String DEV_TYPE_IPHONE="iphone";

    /** Image types */
    public static final int MEDIATYPE_UNKNOWN = 0;
    public static final int MEDIATYPE_JPEG = 1;
    public static final int MEDIATYPE_PNG = 2;
    public static final int MEDIATYPE_BMP = 3;
    public static final int MEDIATYPE_TIFF = 4;
    public static final int MEDIATYPE_GIF = 5;
    public static final int MEDIATYPE_SWF = 6;
    public static final int MEDIATYPE_PDF = 7;
    public static final int MEDIATYPE_FLV = 8;
    public static final int MEDIATYPE_MP4 = 9;
    public static final int MEDIATYPE_GALLERY = 10;

    private String devKey;
    private String baseUrl;

    /**
     * <Constructor>
     * @param key <shared key obtained from YFrog>
     * you can get more information how to request a key if follow <a href="http://code.google.com/p/imageshackapi/wiki/DeveloperKey">
     */
    public YFrogConnector(String key) {
        this.devKey = key;
        this.baseUrl = "http://yfrog.com";
    }
    /**
     * <description>
     * @param key <shared key obtained from YFrog>
     * @param domain <Country specific domain code>
     * see a list of available domains <a href="http://code.google.com/p/imageshackapi/wiki/YFROGurls">
     */
    public YFrogConnector(String key, String domain) {
        this.devKey = key;
        this.baseUrl = "http://"+domain;
    }
    /**
     * <Identifies media type by URL or ID>
     * @param UrlOrID <media object URL or ID >
     * @return <media type identified by UrlOrID parameter, one of the MEDIAYTYPE constants>
     */
    public static int YFROGgetMediaType(String UrlOrID) {
        if ((UrlOrID == null) || (UrlOrID.length() == 0))
            return MEDIATYPE_UNKNOWN;
        switch (UrlOrID.charAt(UrlOrID.length() - 1)) {
            case 'j': return MEDIATYPE_JPEG;
            case 'p': return MEDIATYPE_PNG;
            case 'b': return MEDIATYPE_BMP;
            case 't': return MEDIATYPE_TIFF;
            case 'g': return MEDIATYPE_GIF;
            case 's': return MEDIATYPE_SWF;
            case 'd': return MEDIATYPE_PDF;
            case 'f': return MEDIATYPE_FLV;
            case 'z': return MEDIATYPE_MP4;
            case 'x': return MEDIATYPE_GALLERY;
        }
        return MEDIATYPE_UNKNOWN;
    }

    /**
     * <Identifies if ID or URL specifies link to image>
     * @param UrlOrID <ID or URL>
     * @return <true if image, otherwise false>
     */
    public static boolean YFROGisimageUrl(String UrlOrID) {
        switch (YFROGgetMediaType(UrlOrID)) {
            case MEDIATYPE_JPEG:
            case MEDIATYPE_PNG:
            case MEDIATYPE_BMP:
            case MEDIATYPE_TIFF:
            case MEDIATYPE_GIF:
                return true;
        }
        return false;
    }

    /**
     * <Identifies if ID or URL specifies link to video>
     * @param UrlOrID <ID or URL>
     * @return <true if video, otherwise false>
     */
    public static boolean YFROGisvideoUrl(String UrlOrID) {
        switch (YFROGgetMediaType(UrlOrID)) {
            case MEDIATYPE_MP4:
                return true;
        }
        return false;
    }
/**
 * <Return URL of optimized image>
 * @param UrlOrID <Image URL or ID>
 * @param deviceType <one of the DEV_TYPE constants>
 * @return <URL which points to optimized image>
 */
    public String YFROGoptimizedimageUrl(String UrlOrID, String deviceType) {
        if ((UrlOrID == null) || (UrlOrID.length() == 0))
            return null;
        if (!UrlOrID.startsWith("http://yfrog"))
            UrlOrID = baseUrl + "/" + UrlOrID;
        if (YFROGisimageUrl(UrlOrID)) {
            StringBuffer res = new StringBuffer(UrlOrID);
            if ((deviceType != null) && (deviceType.length() > 0)) {
                res.append(':');
                res.append(deviceType);
            }
            return res.toString();
        }
        return null;
    }
    /**
     * <Get video frame URL >
     * @param UrlOrID <ID or URL>
     * @return <URL which points to video frame image>
     */
    public String YFROGvideoframeUrl(String UrlOrID) {
        if ((UrlOrID == null) || (UrlOrID.length() == 0))
            return null;
        if (!UrlOrID.startsWith("http://yfrog"))
            UrlOrID = baseUrl + "/" + UrlOrID;
        if (YFROGisvideoUrl(UrlOrID))
            return (UrlOrID + ":frame");
        return null;
    }

    /**
     * <Get thumbnail URL >
     * @param UrlOrID <ID or URL>
     * @return <URL which points to thumbnail image>
     */
    public String YFROGthumbnailUrl(String UrlOrID) {
        if ((UrlOrID == null) || (UrlOrID.length() == 0))
            return null;
        if (!UrlOrID.startsWith("http://yfrog"))
            UrlOrID = baseUrl + "/" + UrlOrID;
        if (YFROGisimageUrl(UrlOrID) || YFROGisvideoUrl(UrlOrID))
            return (UrlOrID + ".th.jpg");
        return null;
    }

    /**
     * <Upload image to YFrog server>
     * see additional information at <a href="http://code.google.com/p/imageshackapi/wiki/YFROGuploadAndPost">
     * @param data <image data>
     * @param url <URL of image. Either media or url parameter is required>
     * @param username <Twitter username>
     * @param password <Twitter password>
     * @param oAuthMode <true for oAuth mode >
     * @param oAuthVerifyUrl <if oAuth is true then verify url, otherwise null>
     * @param tags <tags associated with image>
     * @param isPublic <if image is public then true, false otherwise>
     * @return <network ID>
     */
    public long YFROGupload(byte[] data, String url, String username, String password, boolean oAuthMode, String oAuthVerifyUrl, String[] tags, boolean isPublic)    {
    /*
    * media - Binary image or video data; either media or url parameter is required
    * url - URL of image or video; either media or url parameter is required
    * username (required) - Twitter username
    * password (required) - Twitter password
    * tags (optional) - comma-separated list of tags. (tags can also include geo tags)
    * public (optional) - Public/private marker of your video/picture. yes means public (default), no means private
    * key - DeveloperKey.
    */
        Vector parts = new Vector();
        parts.addElement(
            new ContentMultipartFormData.MimePart(
                null,
                "form-data; name=\"key\"",
                devKey));
        parts.addElement(
            new ContentMultipartFormData.MimePart(
                null,
                "form-data; name=\"username\"",
                username));
        if (oAuthMode) {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"auth\"",
                    "oauth"));
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"verify_url\"",
                    oAuthVerifyUrl));
        }
        else {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"password\"",
                    password));
        }
        parts.addElement(
            new ContentMultipartFormData.MimePart(
                null,
                "form-data; name=\"public\"",
                isPublic?"yes":"no"));
        if( url!=null ) {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"url\"",
                    url));
        }
        if( tags!=null && tags.length>0 ) {
            StringBuffer sb = new StringBuffer();
            for(int i=0; i<tags.length; i++) {
                if(i>0)
                    sb.append(",");
                sb.append(tags[i]);
            }
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"tags\"",
                    sb.toString()));
        }
        if( data!=null ) {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    "application/octet-stream",
                    "form-data; name=\"media\"; filename=\"blackberry.jpg\"",
                    data));
        }
        ContentMultipartFormData.MimePart[] arrParts = new ContentMultipartFormData.MimePart[parts.size()];
        for(int i=0; i<parts.size(); i++)
            arrParts[i] = (ContentMultipartFormData.MimePart)parts.elementAt(i);

        ContentMultipartFormData contentProvider = new ContentMultipartFormData(arrParts, true);
        long res = NetworkManager.getInstance().startRequest(HttpMethod.POST, "http://yfrog.com/api/upload", null, contentProvider, null,
            new ContentProcessor() {
                public Object process(
                    NetworkRequest request,
                    int responseCode, Hashtable responseHeaders, InputStream responseData
                ) throws Exception {
                    YFrogXmlHandler handler = new YFrogXmlHandler();
                    try { SAXParserFactory.newInstance().newSAXParser().parse(responseData, handler); }
                    catch (Exception ex) { throw ex; }
                    Response rsp = (Response)handler.getResult();
                    if( rsp.errCode!=Response.SUCCESS )
                        throw new Exception(rsp.errMessage);
                    return rsp;
                }
        });
        return res;
    }
    /**
     * <Upload media by chunks>
     * see additional information at  <a href="http://code.google.com/p/imageshackapi/wiki/ChunkedVideoUploadApiI">
     * @param data <media data>
     * @param filename <name of file to be uploaded, optional. Used to determine target file name, if not specified, video.....mp4 will be used as destination file name>
     * @param cookie <user's cookies, optional. If specified, video will be linked to user's account>
     * @param a_username <Imageshack user's name, optional>
     * @param a_password <Imageshack user's password, optional>
     * @param tags <comma-separated list of tags>
     * @param isPublic <specifies if media is public>
     * @return <network request ID>
     */
    public long YFROGuploadByChunks(byte[] data, String filename, String cookie, String a_username, String a_password, String[] tags, boolean isPublic)    {
        Vector parts = new Vector();
        parts.addElement(
            new ContentMultipartFormData.MimePart(
                null,
                "form-data; name=\"key\"",
                devKey));
        if( a_username!=null ) {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"username\"",
                    a_username));
        }
        if( a_password!=null ) {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"password\"",
                    a_password));
        }
        parts.addElement(
            new ContentMultipartFormData.MimePart(
                null,
                "form-data; name=\"public\"",
                isPublic?"yes":"no"));
        if( cookie!=null ) {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"cookie\"",
                    cookie));
        }
        if( tags!=null && tags.length>0 ) {
            StringBuffer sb = new StringBuffer();
            for(int i=0; i<tags.length; i++) {
                if(i>0)
                    sb.append(",");
                sb.append(tags[i]);
            }
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    null,
                    "form-data; name=\"tags\"",
                    sb.toString()));
        }
        if( data!=null ) {
            parts.addElement(
                new ContentMultipartFormData.MimePart(
                    "application/octet-stream",
                    "form-data; name=\"media\"; filename=\""+filename+"\"",
                    data));
        }
        ContentMultipartFormData.MimePart[] arrParts = new ContentMultipartFormData.MimePart[parts.size()];
        for(int i=0; i<parts.size(); i++)
            arrParts[i] = (ContentMultipartFormData.MimePart)parts.elementAt(i);

        ContentMultipartFormData contentProvider = new ContentMultipartFormData(arrParts, false);

        // define multistages content processor
        ContentProcessor contentProcessor = new ContentProcessor() {
                public Object process(
                    NetworkRequest request,
                    int responseCode, Hashtable responseHeaders, InputStream responseData
                ) throws Exception {
                    if( responseCode==202 ) {
                        // continue to upload data
                        request.startNext(HttpMethod.PUT, request.getUrl(), null, request.getPostContent(),
                            null, request.getContentProcessor());
                        return null;
                    }
                    if( responseCode==200 ) {
                        //String reply = codeminders.yfrog.lib.utils.StreamUtils.readStreamContentToString(responseData, "UTF-8");
                        ////System.out.println(reply);
                        YFrogXmlHandler handler = new YFrogXmlHandler();
                        try { SAXParserFactory.newInstance().newSAXParser().parse(responseData, handler); }
                        catch (Exception ex) { throw ex; }
                        UploadInfo uinfo = (UploadInfo)handler.getResult();
                        // initiate first PUT request
                        ContentMultipartFormData cp = (ContentMultipartFormData)request.getPostContent();
                        ContentMultipartFormData.MimePart binaryPart = cp.getFirstBinaryPart();
                        ContentProviderChunkedData chunksProvider =  new ContentProviderChunkedData(
                                binaryPart.contentType,
                                binaryPart.binData, 32000 );
                        request.startNext(HttpMethod.PUT, uinfo.putURL, null, chunksProvider, null, request.getContentProcessor());
                        return null;
                    }
                    if( responseCode==201 ) {
//                        String reply = codeminders.yfrog.lib.utils.StreamUtils.readStreamContentToString(responseData, "UTF-8");
//                        //System.out.println(reply);

                        YFrogXmlHandler handler = new YFrogXmlHandler();
                        try { SAXParserFactory.newInstance().newSAXParser().parse(responseData, handler); }
                        catch (Exception ex) { throw ex; }
                        Object rsp = handler.getResult();
                        if( rsp instanceof ImgInfo) {
                            ImgInfo imginfo = (ImgInfo)rsp;
                            return imginfo;
                        }
                        if( rsp instanceof PutError ) {
                            PutError err = (PutError)rsp;
                            throw new Exception("Error uploading media, code: "+err.code+" message: "+err.message);
                        }
                        throw new Exception("Uploading response not identified");
                    }
                    return null;
                }};

        // initiate first stage
        return NetworkManager.getInstance().startRequest(HttpMethod.POST, "http://render.imageshack.us/renderapi/start",
                        null, contentProvider, null, contentProcessor);
    }

    /**
     * <Retrieve media detailed info>
     * @param UrlOrID <URL or ID>
     * @return <network ID>
     */
    public long YFROGxmlInfo(String UrlOrID) {
        String id = getId(UrlOrID);
        String url = "http://yfrog.com/api/xmlInfo?path="+id;

        long res = NetworkManager.getInstance().startRequest(HttpMethod.GET, url, null, null, null,
            new ContentProcessor() {
                public Object process(
                    NetworkRequest request,
                    int responseCode, Hashtable responseHeaders, InputStream responseData
                ) throws Exception {
                    YFrogXmlHandler handler = new YFrogXmlHandler();
                    try { SAXParserFactory.newInstance().newSAXParser().parse(responseData, handler); }
                    catch (Exception ex) { throw ex; }
                    Object rsp = handler.getResult();
                    if( rsp instanceof ImgInfo )
                        return rsp;
                    if( rsp instanceof Links ) {
                        Links links = (Links)rsp;
                        throw new Exception(links.error);
                    }
                    return null;
                }
        });

        return res;
    }

    /**
     * <description>
     * If UrlOrID contains Url then extracts it, otherwise returns base url
     */
    private String getUrl(String UrlOrID) {
        if( UrlOrID.startsWith("http://yfrog") ) {
            int pos = UrlOrID.lastIndexOf('/');
            return UrlOrID.substring(0,pos-1);
        }
        else
            return baseUrl;
    }
    public static String getId(String UrlOrID) {
        String res;
        if (UrlOrID.startsWith("http://yfrog"))
            res = UrlOrID.substring(UrlOrID.lastIndexOf('/') + 1);
        else
            res = UrlOrID;
        int pos = res.lastIndexOf(':');
        if (pos >= 0)
            res = res.substring(0, pos);
        return res;
    }
}
