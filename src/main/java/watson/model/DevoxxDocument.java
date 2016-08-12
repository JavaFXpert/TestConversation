package watson.model;

/**
 * Created by jamesweaver on 8/12/16.
 */
public class DevoxxDocument {
  private String id;
  private String label;
  private String score;
  private String authors;
  private String emotions;
  private String language;
  private String link;
  private String publicationDate;
  private String sentiment;
  private String thumbnail;
  private String thumbnailKeywords;

  public DevoxxDocument() {
  }

  public DevoxxDocument(String id, String label, String score, String authors, String emotions, String language, String link, String publicationDate, String sentiment, String thumbnail, String thumbnailKeywords) {
    this.id = id;
    this.label = label;
    this.score = score;
    this.authors = authors;
    this.emotions = emotions;
    this.language = language;
    this.link = link;
    this.publicationDate = publicationDate;
    this.sentiment = sentiment;
    this.thumbnail = thumbnail;
    this.thumbnailKeywords = thumbnailKeywords;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getScore() {
    return score;
  }

  public void setScore(String score) {
    this.score = score;
  }

  public String getAuthors() {
    return authors;
  }

  public void setAuthors(String authors) {
    this.authors = authors;
  }

  public String getEmotions() {
    return emotions;
  }

  public void setEmotions(String emotions) {
    this.emotions = emotions;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(String publicationDate) {
    this.publicationDate = publicationDate;
  }

  public String getSentiment() {
    return sentiment;
  }

  public void setSentiment(String sentiment) {
    this.sentiment = sentiment;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  public String getThumbnailKeywords() {
    return thumbnailKeywords;
  }

  public void setThumbnailKeywords(String thumbnailKeywords) {
    this.thumbnailKeywords = thumbnailKeywords;
  }

  @Override
  public String toString() {
    return "DevoxxDocument{" +
        "id='" + id + '\'' +
        ", label='" + label + '\'' +
        ", score='" + score + '\'' +
        ", authors='" + authors + '\'' +
        ", emotions='" + emotions + '\'' +
        ", language='" + language + '\'' +
        ", link='" + link + '\'' +
        ", publicationDate='" + publicationDate + '\'' +
        ", sentiment='" + sentiment + '\'' +
        ", thumbnail='" + thumbnail + '\'' +
        ", thumbnailKeywords='" + thumbnailKeywords + '\'' +
        '}';
  }
}
