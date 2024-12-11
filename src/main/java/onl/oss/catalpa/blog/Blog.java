package onl.oss.catalpa.blog;

import onl.oss.catalpa.CacheManager;
import onl.oss.catalpa.GeneratorException;
import onl.oss.catalpa.Util;
import onl.oss.catalpa.model.Content;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static onl.oss.catalpa.Logger.WARN;

public class Blog implements Cloneable {

    private final Path path;
    private final List<Post> posts;
    private final List<Category> categories;
    private final List<Page> pages;

    private Map<Path, Post> postByPath;
    private Post post;
    private Page page;
    private Category category;

    private Blog(Path path, Map<Path, Post> postByPath, List<Post> posts, List<Category> categories, List<Page> pages) {
        this.postByPath = postByPath;
        this.path = path;
        this.posts = posts;
        this.categories = categories;
        this.pages = pages;
    }

    public Path getPath() {
        return path;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Page> getPages() {
        return pages;
    }

    public Post getPostBy(Path path) {
        return postByPath.get(path);
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Post getPost() {
        return post;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public Blog clone() {
        return new Blog(path, postByPath, posts, categories, pages);
    }


    /** 指定したパスからブログ・フォルダーを検索します。
     * config.yml に type: blog が定義されているフォルダーがブログ・フォルダーです。
     *
     * @param input 検索するパス
     * @return ブログ・フォルダーのパス、見つからない場合は null
     * @throws IOException 例外
     */
    public static Path findPath(Path input) throws IOException {
        return null;
    }

    public static Blog create(Path input) throws IOException {
        //
        // ブログ・フォルダーを検索します。
        //
        Content blogConfig = null;

        List<Path> config = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(input)) {
            stream.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String filename = path.getFileName().toString();
                    if (filename.equalsIgnoreCase("config.yml")) {
                        config.add(path);
                    }
                }
            });
        }

        for (Path file : config) {
            FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            Content content = CacheManager.getContent(file, lastModifiedTime);
            if (content == null) {
                content = new Content(file);
                CacheManager.putContent(file, lastModifiedTime, content);
            }

            if (content.getYaml().get("type") instanceof String type) {
                if (type.equalsIgnoreCase("blog")) {
                    blogConfig = content;
                    break;
                }
            }
        }

        if (blogConfig == null) {
            return null;
        }

        Map<Path, Post> postByPath = new HashMap<>();
        List<Post> posts = new ArrayList<>();
        List<Category> categories = new ArrayList<>();

        //
        // 記事
        //
        try (Stream<Path> stream = Files.walk(blogConfig.getPath().getParent())) {
            List<Post> list = Collections.synchronizedList(posts);
            stream.parallel().forEach(path -> {
                try {
                    if (Util.isMarkdownFile(path)) {
                        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                        Content content = CacheManager.getContent(path, lastModifiedTime);
                        if (content == null) {
                            content = new Content(path);
                            CacheManager.putContent(path, lastModifiedTime, content);
                        }
                        Post post = getPost(input, content);
                        if (post != null) {
                            list.add(post);
                        }
                    }
                } catch (Exception e) {
                    throw new GeneratorException(path, e);
                }
            });
        }

        //
        // ドラフト
        //

        // ドラフト記事が存在する場合は、それ以外の記事にスキップを設定します。
        for (Post post1 : posts) {
            if (post1.isDraft()) {
                System.out.println("draft: " + post1);
                for (Post post2 : posts) {
                    if (!post2.isDraft()) {
                        System.out.println(" skip: " + post2);
                        post2.setSkip(true);
                    }
                }
                break;
            }
        }

        // スキップが設定されている記事を取り除きます。
        for (int i = posts.size() - 1; i >= 0; i--) {
            Post post = posts.get(i);
            if (post.isSkip()) {
                posts.remove(i);
            }
        }

        System.out.println("posts.size()=" + posts.size());

        posts.sort(Comparator.comparing(Post::getDate).thenComparing(Post::getLastModifiedTime).reversed());


        //
        // 記事のカテゴリーIDを補完します。
        //
        int categoryId = 0;
        Map<String, Category> map = new HashMap<>();
        for (Post post : posts) {
            for (Category category : post.getCategories()) {
                String name = category.getName();
                if (category.getId() != null && !map.containsKey(name)) {
                    map.put(name, category);
                }
            }
        }
        for (Post post : posts) {
            Set<Category> set = new LinkedHashSet<>();
            for (Category category : post.getCategories()) {
                String name = category.getName();
                Category c = map.get(name);
                if (c == null) {
                    // カテゴリーに ID が指定されていない場合、N を自動的に割り当てます。（N は 1 から始まる連番です）
                    c = new Category(Integer.toString(++categoryId), name);
                    map.put(name, c);
                }
                set.add(c);
            }
            post.getCategories().clear();
            post.getCategories().addAll(set);
        }


        //
        // 前の記事、次の記事
        //
        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);

            int p = i + 1;
            if (p < posts.size()) {
                Post previous = posts.get(p);
                post.setPrevious(previous);
            }

            int n = i - 1;
            if (n >= 0) {
                Post next = posts.get(n);
                post.setNext(next);
            }
        }


        //
        // カテゴリー
        //
        for (Post post : posts) {
            for (Category category : post.getCategories()) {
                if (!categories.contains(category)) {
                    categories.add(category);
                }
                category.getPosts().add(post);
            }
        }

        for (Category category : categories) {
            Path path = blogConfig.getPath().getParent().resolve("category").resolve(category.getId()).resolve("index.html");
            String relative = input.relativize(path).toString().replace('\\', '/');
            String url = URLEncoder.encode(relative, StandardCharsets.UTF_8).replace("%2F", "/");
            category.setUrl(url);
            category.getPosts().sort(Comparator.comparing(Post::getDate).thenComparing(Post::getLastModifiedTime).reversed());
        }

        categories.sort(Comparator.comparing(Category::getLastDate).thenComparing(c -> c.getPosts().size()).reversed());


        //
        // 関連記事
        //
        for (Post post : posts) {
            List<Map.Entry<Integer, Post>> list = new LinkedList<>();
            for (Post other : posts) {
                int score = 0;

                if (other == post) {
                    continue;
                }

                for (Category category : post.getCategories()) {
                    for (Category otherCategory : other.getCategories()) {
                        if (category.getId().equals(otherCategory.getId())) {
                            score += 100000;
                        }
                    }
                }

                int days = (int) ChronoUnit.DAYS.between(post.getDate(), other.getDate());
                if (days > 0) {
                    score += days * (-2) + 1;
                } else if (days < 0) {
                    score += days * 2;
                }

                list.add(Map.entry(score, other));
            }

            list.sort((o1, o2) -> o2.getKey() - o1.getKey());
            List<Post> related = new ArrayList<>();
            for (Map.Entry<Integer, Post> entry : list) {
                int score = entry.getKey();
                Post other = entry.getValue();
                if (score > 50000) {
                    related.add(other);
                }
            }
            post.setRelated(related);
        }


        //
        // インデックス・ページの作成
        //
        int paginate = 12;
        {
            Object obj = blogConfig.getYaml().get("paginate");
            if (obj != null) {
                int i = 0;
                try {
                    i = Integer.parseInt(obj.toString());
                } catch (NumberFormatException ignored) {}

                if (i <= 0) {
                    throw new GeneratorException(blogConfig.getPath(), "paginate に無効な値が指定されています: " + obj);
                }

                paginate = i;
            }
        }

        List<Page> pages = new ArrayList<>();
        int current = 1;
        int fromIndex = 0;
        int toIndex;
        while (fromIndex < posts.size()) {
            toIndex = Math.min(fromIndex + paginate, posts.size());

            Path path;
            if (current == 1) {
                path = blogConfig.getPath().getParent().resolve("index.html");
            } else {
                path = blogConfig.getPath().getParent().resolve("page").resolve(current + ".html");
            }
            String relative = input.relativize(path).toString().replace('\\', '/');
            String url = URLEncoder.encode(relative, StandardCharsets.UTF_8).replace("%2F", "/");
            String name = Integer.toString(current);

            Page page = new Page(url, name, posts.subList(fromIndex, toIndex));
            pages.add(page);

            current++;
            fromIndex = toIndex;
        }

        //
        // 前のインデックス・ページ、次のインデックス・ページ
        //
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);

            int p = i - 1;
            if (p >= 0) {
                Page previous = pages.get(p);
                page.setPrevious(previous);
            }

            int n = i + 1;
            if (n < pages.size()) {
                Page next = pages.get(n);
                page.setNext(next);
            }
        }

        Path path = blogConfig.getPath().getParent();
        return new Blog(path, postByPath, posts, categories, pages);
    }

    private static Post getPost(Path input, Content content) {
        //
        // date
        //
        // date が定義されていないMarkdownファイルは記事として扱いません（nullを返します）
        Object obj = content.getYaml().get("date");
        if (obj == null) {
            return null;
        }
        LocalDate date = LocalDate.parse(obj.toString());

        //
        // title
        //
        String title = "";
        if (content.getYaml().get("title") instanceof String s) {
            title = s;
        }

        //
        // categories
        //
        Set<Category> categories = new LinkedHashSet<>();
        if (content.getYaml().get("categories") instanceof List<?> list) {
            for (Object e : list) {
                if (e != null) {
                    Category category = new Category(e.toString());
                    categories.add(category);
                }
            }
        }

        //
        // path
        //
        Path path = content.getPath();

        //
        // url
        //
        String filename = path.getFileName().toString();
        int i = filename.lastIndexOf('.');
        if (i < 0) {
            filename = filename + ".html";
        } else {
            filename = filename.substring(0, i) + ".html";
        }
        Path relativeInputPath = input.relativize(content.getPath());
        Path relativeOutputPath = relativeInputPath.resolveSibling(filename);
        // URLEncoder.encode ではパス区切り文字 "/" が "%2F" にエンコードされてしまうので、"%2F" を "/" に戻しています。
        String url = URLEncoder.encode(relativeOutputPath.toString().replace('\\', '/'), StandardCharsets.UTF_8).replace("%2F", "/");

        //
        // thumbnail
        //
        String thumbnail = null;
        {
            Path file = null;

            // YAML に thumbnail が定義されていればそれを採用します。（ファイルが存在しなくても無条件で採用されます）
            obj = content.getYaml().get("thumbnail");
            if (obj != null) {
                String s = obj.toString();
                if (!s.isEmpty()) {
                    file = (s.charAt(0) == '/') ? input.resolve(s.substring(1)) : path.getParent().resolve(s);
                    /*
                     */
                }
            }

            // YAML に thumbnail が定義されていない場合は、同じフォルダーに "thumbnail.webp" などの候補ファイルが存在しているか確認して、存在するファイルを採用します。
            if (file == null) {
                String[] candidates = new String[] {
                        "thumbnail.webp", "thumbnail.png", "thumbnail.jpg", "thumbnail.jpeg", "thumbnail.gif",
                        "image.webp", "image.png", "image.jpg", "image.jpeg", "image.gif"};

                for (String candidate : candidates) {
                    Path f = path.getParent().resolve(candidate);
                    if (Files.exists(f)) {
                        file = f;
                        break;
                    }
                }
            }

            if (file != null) {
                String relative = input.relativize(file).toString().replace('\\', '/');
                thumbnail = URLEncoder.encode(relative, StandardCharsets.UTF_8).replace("%2F", "/");
            }
        }

        //
        // image
        //
        String image = null;
        {
            Path file = null;
            // YAML に image が定義されていればそれを採用します。（ファイルが存在しなくても無条件で採用されます）
            obj = content.getYaml().get("image");
            if (obj != null) {
                String s = obj.toString();
                if (!s.isEmpty()) {
                    file = (s.charAt(0) == '/') ? input.resolve(s.substring(1)) : path.getParent().resolve(s);
                }
            }

            // YAML に image が定義されていない場合は、同じフォルダーに "image.webp" などの候補ファイルが存在しているか確認して、存在するファイルを採用します。
            if (file == null) {
                String[] candidates = new String[] {
                        "image.webp", "image.png", "image.jpg", "image.jpeg", "image.gif",
                        "thumbnail.webp", "thumbnail.png", "thumbnail.jpg", "thumbnail.jpeg", "thumbnail.gif" };

                for (String candidate : candidates) {
                    Path f = path.getParent().resolve(candidate);
                    if (Files.exists(f)) {
                        file = f;
                        break;
                    }
                }
            }

            if (file != null) {
                String relative = input.relativize(file).toString().replace('\\', '/');
                image = URLEncoder.encode(relative, StandardCharsets.UTF_8).replace("%2F", "/");
            }
        }

        Post post = new Post(content.getPath(), url, date, title, categories, thumbnail, image);

        //
        // draft
        //
        if (content.getYaml().containsKey("draft")) {
            Object draft = content.getYaml().get("draft");
            if (draft != null && draft.toString().trim().equalsIgnoreCase("skip")) {
                post.setSkip(true);
            } else {
                post.setDraft(true);
            }
        }

        return post;
    }
}
