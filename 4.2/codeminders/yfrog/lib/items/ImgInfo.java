/*
 * ImgInfo.java
 */

package codeminders.yfrog.lib.items;

import java.lang.*;
/**
 *
 */
public class ImgInfo extends YFrogItemBase {
    // data fields
    public Rating rating;
    public Files files;
    public Resolution resolution;
    public VideoInfo videoInfo;
    public Uploader uploader;
    public String Class;
    public String visibility;
    public Links links;

    public ImgInfo() {
        super();
    }
    public static class Rating {
        public String ratings;
        public String avg;
    }
    public static class Files {
        public String server;
        public String bucket;
        public String image;
        public int imageSize;
        public String imageContentType;
        public String thumb;
        public int thumbSize;
        public String thumbContentType;
        public String frame;
        public int frameSize;
        public String frameContentType;
    }
    public static class Resolution {
        public int width;
        public int height;
    }
    public static class VideoInfo {
        public String status;
        public int duration;
    }
    public static class Uploader {
        public String ip;
        public String cookie;
    }
    public static class Links {
        public String image_link;
        public String thumb_html;
        public String thumb_bb;
        public String thumb_bb2;
        public String yfrog_link;
        public String yfrog_thumb;
        public String frame_link;
        public String frame_html;
        public String frame_bb;
        public String frame_bb2;
        public String video_embed;
        public String ad_link;
        public String done_page;

        public Links() {
        }
    }
}
