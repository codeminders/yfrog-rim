/*
 * YFrogXmlHandler.java
 *
 */

package codeminders.yfrog.lib;

import java.io.*;
import java.util.*;
import net.rim.device.api.util.*;
import net.rim.device.api.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import codeminders.yfrog.lib.items.*;
/**
 *
 */
public class YFrogXmlHandler extends DefaultHandler {
        private Object rootObject = null;
        private StringBuffer nodeText = null;

        public YFrogXmlHandler() {
            super();
        }
        public Object getResult() {
            return rootObject;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            nodeText = null;

            if (rootObject == null) {
                if( "rsp".equals(localName) ) {
                    Response rsp = new Response();
                    rsp.stat = attributes.getValue("stat");
                    rootObject = rsp;
                    return;
                }
                if( "uploadInfo".equals(localName) ) {
                    UploadInfo uinfo = new UploadInfo();
                    uinfo.link  = attributes.getValue("link");
                    uinfo.putURL = attributes.getValue("putURL");
                    uinfo.getlengthURL = attributes.getValue("getlengthURL");
                    rootObject = uinfo;
                    return;
                }
                if( "error".equals(localName) ) {
                    PutError err = new PutError();
                    err.code = attributes.getValue("code");
                    rootObject = err;
                    return;
                }
                if( "imginfo".equals(localName) ) {
                    ImgInfo iinfo = new ImgInfo();
                    rootObject = iinfo;
                    return;
                }
                if( "links".equals(localName) ) {
                    Links links = new Links();
                    rootObject = links;
                    return;
                }
            }
            if (rootObject == null)
                return;

            if( rootObject instanceof ImgInfo) {
                ImgInfo ii = (ImgInfo)rootObject;
                if( "rating".equals(localName) ) {
                    ii.rating = new ImgInfo.Rating();
                    return;
                }
                if( "files".equals(localName) ) {
                    ii.files = new ImgInfo.Files();
                    ii.files.server = attributes.getValue("server");
                    ii.files.bucket = attributes.getValue("bucket");
                    return;
                }
                if( "image".equals(localName) ) {
                    ii.files.imageSize = Integer.parseInt(attributes.getValue("size"));
                    ii.files.imageContentType = attributes.getValue("content-type");
                    return;
                }
                if( "thumb".equals(localName) ) {
                    ii.files.thumbSize = Integer.parseInt(attributes.getValue("size"));
                    ii.files.thumbContentType = attributes.getValue("content-type");
                    return;
                }
                if( "frame".equals(localName) ) {
                    ii.files.frameSize = Integer.parseInt(attributes.getValue("size"));
                    ii.files.frameContentType = attributes.getValue("content-type");
                    return;
                }
                if( "resolution".equals(localName) ) {
                    ii.resolution = new ImgInfo.Resolution();
                    return;
                }
                if( "video-info".equals(localName) ) {
                    ii.videoInfo = new ImgInfo.VideoInfo();
                    return;
                }
                if( "uploader".equals(localName) ) {
                    ii.uploader = new ImgInfo.Uploader();
                    return;
                }
                if( "links".equals(localName) ) {
                    ii.links = new ImgInfo.Links();
                    return;
                }
            }
            if( (rootObject instanceof Response) && "err".equals(localName) ) {
                Response rsp = (Response)rootObject;
                rsp.errCode = Integer.parseInt(attributes.getValue("code"));
                rsp.errMessage = attributes.getValue("msg");
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if( rootObject!=null ) {
                if(rootObject instanceof Response) {
                    Response rsp = (Response)rootObject;
                    if( "mediaid".equals(localName) && nodeText!=null ) {
                        rsp.mediaid = nodeText.toString();
                        return;
                    }
                    if( "mediaurl".equals(localName) && nodeText!=null ) {
                        rsp.mediaurl = nodeText.toString();
                        return;
                    }
                }
                if( rootObject instanceof UploadInfo ) {
                    // nothing to parse
                    return;
                }
                if( (rootObject instanceof PutError) && nodeText!=null ) {
                    PutError err = (PutError)rootObject;
                    err.message = nodeText.toString();
                    return;
                }
                if( (rootObject instanceof ImgInfo) && nodeText!=null ) {
                    ImgInfo iinfo = (ImgInfo)rootObject;
                    if("class".equals(localName)) {
                        iinfo.Class = nodeText.toString();
                        return;
                    }
                    if("visibility".equals(localName)) {
                        iinfo.visibility = nodeText.toString();
                        return;
                    }
                    if( iinfo.rating!=null ) {
                        if("ratings".equals(localName)) {
                            iinfo.rating.ratings = nodeText.toString();
                            return;
                        }
                        if("avg".equals(localName)) {
                            iinfo.rating.avg = nodeText.toString();
                            return;
                        }
                    }
                    if( iinfo.files!=null ) {
                        if("image".equals(localName)) {
                            iinfo.files.image = nodeText.toString();
                            return;
                        }
                        if("thumb".equals(localName)) {
                            iinfo.files.thumb = nodeText.toString();
                            return;
                        }
                        if("frame".equals(localName)) {
                            iinfo.files.frame = nodeText.toString();
                            return;
                        }
                    }
                    if( iinfo.resolution!=null ) {
                        if("width".equals(localName)) {
                            iinfo.resolution.width = Integer.parseInt(nodeText.toString());
                            return;
                        }
                        if("heigth".equals(localName)) {
                            iinfo.resolution.height = Integer.parseInt(nodeText.toString());
                            return;
                        }
                    }
                    if( iinfo.videoInfo!=null ) {
                        if("status".equals(localName)) {
                            iinfo.videoInfo.status = nodeText.toString();
                            return;
                        }
                        if("duration".equals(localName)) {
                            iinfo.videoInfo.duration = Integer.parseInt(nodeText.toString());
                            return;
                        }
                    }
                    if( iinfo.uploader!=null ) {
                        if("ip".equals(localName)) {
                            iinfo.uploader.ip = nodeText.toString();
                            return;
                        }
                        if("cookie".equals(localName)) {
                            iinfo.uploader.cookie = nodeText.toString();
                            return;
                        }
                    }
                    if( iinfo.links!=null ) {
                        if("image_link".equals(localName)) {
                            iinfo.links.image_link = nodeText.toString();
                            return;
                        }
                        if("thumb_html".equals(localName)) {
                            iinfo.links.thumb_html = nodeText.toString();
                            return;
                        }
                        if("thumb_bb".equals(localName)) {
                            iinfo.links.thumb_bb = nodeText.toString();
                            return;
                        }
                        if("thumb_bb2".equals(localName)) {
                            iinfo.links.thumb_bb2 = nodeText.toString();
                            return;
                        }
                        if("yfrog_link".equals(localName)) {
                            iinfo.links.yfrog_link = nodeText.toString();
                            return;
                        }
                        if("yfrog_thumb".equals(localName)) {
                            iinfo.links.yfrog_thumb = nodeText.toString();
                            return;
                        }
                        if("frame_link".equals(localName)) {
                            iinfo.links.frame_link = nodeText.toString();
                            return;
                        }
                        if("frame_html".equals(localName)) {
                            iinfo.links.frame_html = nodeText.toString();
                            return;
                        }
                        if("frame_bb".equals(localName)) {
                            iinfo.links.frame_bb = nodeText.toString();
                            return;
                        }
                        if("frame_bb2".equals(localName)) {
                            iinfo.links.frame_bb2 = nodeText.toString();
                            return;
                        }
                        if("video_embed".equals(localName)) {
                            iinfo.links.video_embed = nodeText.toString();
                            return;
                        }
                        if("ad_link".equals(localName)) {
                            iinfo.links.ad_link = nodeText.toString();
                            return;
                        }
                        if("done_page".equals(localName)) {
                            iinfo.links.done_page = nodeText.toString();
                            return;
                        }
                    }
                    return;
                }
                if(rootObject instanceof Links) {
                    Links links = (Links)rootObject;
                    if( "error".equals(localName) && nodeText!=null ) {
                        links.error = nodeText.toString();
                        return;
                    }
                }
            }
            nodeText = null;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (nodeText == null)
                nodeText = new StringBuffer();
            nodeText.append(ch, start, length);
        }
    }

