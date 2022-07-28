# Catalpaカスタムフォント

従来、Catalpa の四分アキは monospace font-size:50% の半角スペースで実現していました。
16px フォントの場合の四分アキは 4px です。font-size:50% とした場合、フォントサイズは 8px となり、半角スペースの幅は 4px になることが期待されます。
しかし、Google Chrome では最小フォントサイズに制限があるため、font-size:50% を指定しても十分に小さなサイズになりません。
フォントサイズは 8px ではなく 10px になってしまい、半角スペースの幅も 5px となってしまいます。

この問題おを解決するために Catalpaカスタムフォントを作成しました。このフォントには半角スペースのグリフが1つだけ含まれています。
半角スペースの幅は四分アキと同じにしてあります。つまり、font-size:50% にしなくても、そのままで四分アキになります。（16pxフォントのときに4pxになります。）
二分アキスペースが必要な場合には font-size:200% を指定します。このとき、line-height:0 も併せて指定しないと文字の高さによって改行に影響がでます。

FontForge で catalpa.sfd を開き、woff2 としてフォントを出力します。

woff2 を base64 に変換します。
Windows の場合は certutil -f -encode catalpa.woff2 catalpa.woff2.base64 のようにして base64 に変換できます。
その後、catalpa.woff2.base64 をテキストエディターで開き、-----BEGIN CERTIFICATE-----、-----END CERTIFICATE-----、および改行を削除します。

この woff2 を base64 にした文字列を css で @font-face として指定します。（フォントを別ファイルにせず、css に埋め込むことができます。）

@font-face {
	font-family: "catalpa";
	src: url("data:application/font-woff;base64,d09GMgABAAAAAAJwAA4AAAAABfQAAAIbAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP0ZGVE0cGhwGYACCQggEEQgKWHYLCgABNgIkAxAEIAWEAQcqGxAFAB6FcZsTTmVyqvCdLvV4+H7u89yb95KUecGzLBBYIGEqV8ii3BqDao1oNaH+sq6d2MP9vGUCPVx0WPOvXLQNfESb6WADTjVPIPfF/r+bfIvwA/hdc2kgC3BswkgDKyxKqJfQEQUkgVAQ7tpnnsII9/CgJNuEgO/nRgA/ur2Ll/+DAKy0kR2JQKCkeK5S5+sLYrc8tPngQUYh1WUAAgAuPRjzeBtDXTu0lup1QABgjAUNGuxkAfvYBHayGwNFERgbhvB7ZvgjgGgJQCQSABA0klsdqxEQwyBBAT0SPVYDCqZYgVJWLKx53Ql4Vh7juD7Gcf2Li+8Xp4A3YXrfrytGc2/a+nXf2yaN5nybJlyzt10b/5SEzBYEz/6392ao9bdf0VtnX+IY5L26Bb0EQb0OlhLkPRYAQEmOAYmEQNFLkAiBQzIAVnspAGMGLCB0HrKA1HkkQGPSX4CiizUCVCtimgW0ujjLJsoyHAIbwmp3UbIFaJzwgWK1AarNseUtWqtjr6GDo5czhUR2RWI4caQgp6AghczMrWwt9e31kZWXA3j3mXDxn7Acm81X22V/36crrwNwF+OKoTtiwPu5PUcGuEIgudExzvCUWRU44cB0LctwJhGQgowc0kBj6vXdgpq0qjS4g9uYPF85SwLNZXLUoLmFVRRq6WqadiYfulGeXSg8Kckz+DhSLZvN5ND/fALDPBEEAICyFwIA") format("woff2");
}

Catalpaカスタムフォントを使用する場合は、@markdown ディレクティブに use_catalpa_font=true を指定します。
これで、Catalpaカスタムフォントを使用するHTMLが出力されるようになります。

（例）
　<div class="content markdown"><!--start-search-target--><@markdown replace_backslash_to_yensign=true use_catalpa_font=true>${content!}</@markdown><!--end-search-target--></div>

