package com.loveeleve.ecology.elasticsearch.model;

public class Article {

    private String id;
    private String title;
    private String content;
    private String category;
    private long price;
    private long publishTime;

    public Article() {
    }

    public Article(String id, String title, String content, String category, long price, long publishTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.price = price;
        this.publishTime = publishTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(long publishTime) {
        this.publishTime = publishTime;
    }
}
