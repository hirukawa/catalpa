package onl.oss.catalpa.blog;

import java.util.List;

public class Page {

    private String url;
    private String name;
    private List<Post> posts;
    private Page previous;
    private Page next;

    public Page(String url, String name, List<Post> posts) {
        this.url = url;
        this.name = name;
        this.posts = posts;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPrevious(Page previous) {
        this.previous = previous;
    }

    public Page getPrevious() {
        return previous;
    }

    public void setNext(Page next) {
        this.next = next;
    }

    public Page getNext() {
        return next;
    }
}
