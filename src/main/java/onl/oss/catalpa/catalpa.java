package onl.oss.catalpa;

/** macOS の jpackage でアプリケーションを作ると、メインクラス名がプロセス名として表示されます。
 * そのため、エントリーポイントであるメインクラス名を Main ではなく catalpa としています。
 *
 */
public class catalpa {
    public static void main(String[] args) {
        Main.main(args);
    }
}
