package onl.oss.catalpa.blog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Category {
    private static final Pattern CATEGORY_ID_PATTERN = Pattern.compile("(.+)\\((.*)\\)$");

    private String id;
    private String name;
    private List<Post> posts = new ArrayList<>();
    private String url;
    private LocalDate lastDate;

    public Category(String text) {
        String id = null;
        String name = text.trim();

        Matcher m = CATEGORY_ID_PATTERN.matcher(text);
        if (m.matches()) {
            name = m.group(1).trim();
            String s = m.group(2).replaceAll("[^-_.a-zA-Z0-9]", "").trim();

            // 数字以外の文字を含んでいれば id として採用します。（数字のみの名前はカテゴリーidの自動生成で予約されているため使用できません）
            if (!s.matches("[0-9]+")) {
                id = s;
            }
        } else {
            // name(id) の形式ではなく、name が 英数字およびハイフン、アンダースコア、ドットのみで構成されている場合は name を id として使用します。
            if (name.matches("[-_.a-zA-Z0-9]+")) {
                id = name.toLowerCase();
            }
        }

        this.id = id;
        this.name = name;
    }

    public Category(String id, String name) {
        this.id = id;
        this.name = name;

    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public LocalDate getLastDate() {
        if (posts.isEmpty()) {
            return LocalDate.EPOCH;
        }

        // 事前に posts がソートされている前提です。
        return posts.getFirst().getDate();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id) && Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", posts=" + posts.size() +
                '}';
    }
}
