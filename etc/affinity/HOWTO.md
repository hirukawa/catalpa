# Affinity Designer で CSS に埋め込む SVG を作るときの注意点

  * SVG 形式でエクスポートするときに「次のDPIを使用」の値をドキュメント設定の DPI に合わせる
    ファイル -> ドキュメント設定 で DPI を 96 ならエクスポート時も 96 を選択する。

  * SVG 形式でエクスポートするときに「16進数カラーを使用」のチェックを外す
    fill:#000000; 形式だとエラーになる。
    上記チェックを外すことで fill:rgb(0,0,0); 形式で出力されるようになる。

  * SVG 形式でエクスポートするときに「viewBoxを設定する」のチェックを入れる

  * SVG 形式でエクスポートするときに「改行を追加」のチェックを外す

# 出力後の SVG から取り除いてもよいもの

  * <?xml?> タグ
  * <!DOCTYPE> タグ

# li::marker の content で使うときの例

li::marker {
    content: url('data:image/svg+xml;utf8,<svg width="1em" height="1em" ...></svg>') "\2003";
}

width と height は 1em に変更するとよい。
