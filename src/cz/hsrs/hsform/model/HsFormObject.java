/**
 *
 */
package cz.hsrs.hsform.model;

import java.io.File;

/**
 * Class models content of POI form
 * @author mkepka
 *
 */
public class HsFormObject {

    private String title;
    private String description;
    private String category;
    private String status;
    private String longitude;
    private String lattitude;
    private String beginTimestamp;
    private String timestamp;
    private String serviceUrl;
    private File picture;

    /**
     * @param title
     * @param description
     * @param category
     * @param status
     * @param longitude
     * @param lattitude
     * @param beginTimestamp
     * @param timestamp
     * @param serviceUrl
     * @param picture
     */
    public HsFormObject(String title, String description, String category,
            String status, String longitude, String lattitude,
            String beginTimestamp, String timestamp, String serviceUrl,
            File picture) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = status;
        this.longitude = longitude;
        this.lattitude = lattitude;
        this.beginTimestamp = beginTimestamp;
        this.timestamp = timestamp;
        this.serviceUrl = serviceUrl;
        this.picture = picture;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return the longitude
     */
    public String getLongitude() {
        return longitude;
    }

    /**
     * @return the lattitude
     */
    public String getLattitude() {
        return lattitude;
    }

    /**
     * @return the timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @return the picturePath
     */
    public String getPicturePath() {
        if(picture != null){
            return picture.getAbsolutePath();
        }
        else{
            return null;
        }
    }

    /**
     * @return the serviceUrl where should be data upload
     */
    public String getServiceUrl(){
        return serviceUrl;
    }

    /**
     * @return the picture
     */
    public File getPicture() {
        return picture;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return the beginTimestamp
     */
    public String getBeginTimestamp() {
        return beginTimestamp;
    }

    /**
     * @return String of all attributes names and values
     */
    @Override
    public String toString() {
        return "HsFormObject [title=" + title + ", description=" + description
                + ", category=" + category + ", status=" + status
                + ", beginTimestamp=" + beginTimestamp + ", longitude=" + longitude
                + ", lattitude=" + lattitude + ", timestamp=" + timestamp
                + ", picturePath=" + getPicturePath() + ", serviceUrl=" + serviceUrl
                + "]";
    }
}
