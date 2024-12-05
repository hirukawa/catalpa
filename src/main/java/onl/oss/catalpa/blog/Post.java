package onl.oss.catalpa.blog;

import org.checkerframework.checker.units.qual.A;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Post {

    private Path path;
    private String url;
    private LocalDate date;
    private String title;
    private Set<Category> categories;
    private String thumbnail;
    private String image;
    private Post previous;
    private Post next;
    private List<Post> related;

    private FileTime lastModifiedTime;

    public Post(Path path, String url, LocalDate date, String title, Set<Category> categories, String thumbnail, String image) {
        this.path = path;
        this.url = url;
        this.date = date;
        this.title = title != null ? title : "";
        this.categories = categories != null ? categories : Collections.emptySet();
        this.thumbnail = thumbnail;
        this.image = image;
        this.related = new ArrayList<>();
    }

    public Path getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getImage() {
        return image;
    }

    public void setPrevious(Post previous) {
        this.previous = previous;
    }

    public Post getPrevious() {
        return previous;
    }

    public void setNext(Post next) {
        this.next = next;
    }

    public Post getNext() {
        return next;
    }

    public void setRelated(List<Post> related) {
        this.related.clear();

        if (related != null) {
            this.related.addAll(related);
        }
    }

    public List<Post> getRelated() {
        return related;
    }

    public FileTime getLastModifiedTime() {
        if (lastModifiedTime == null) {
            try {
                lastModifiedTime = Files.getLastModifiedTime(path);
            } catch (IOException ignored) {}
        }

        if (lastModifiedTime != null) {
            return lastModifiedTime;
        }

        return FileTime.fromMillis(0);
    }

    @Override
    public String toString() {
        String p = null;
        if (previous != null) {
            p = "{date=" + previous.getDate() + ", title='" + previous.getTitle() + "'}";
        }
        String n = null;
        if (next != null) {
            n = "{date=" + next.getDate() + ", title=" + next.getTitle() + "'}";
        }

        return "Post{" +
                "path=" + path +
                ", url='" + url + '\'' +
                ", date=" + date +
                ", title='" + title + '\'' +
                ", categories=" + categories +
                ", thumbnail='" + thumbnail + '\'' +
                ", image='" + image + '\'' +
                ", related=" + related.size() +
                ", previous=" + p +
                ", next=" + n +
                '}';
    }
}
