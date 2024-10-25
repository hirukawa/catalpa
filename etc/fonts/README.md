# Catalpaカスタムフォント

このフォントには半角スペースのグリフのみを持つフォントです。
半角スペース文字が全角文字と同じ幅を持っています。
font-size: 50% とすることで二分アキ、font-size: 25% とすることで四分アキ となります。

※ 以前は半角スペースの幅を四分アキとして、二分アキのときは font-size: 200% としていました。
　 ですが、font-size: 200% とすると行の高さも大きくなり改行に影響が出ていました。（line-height:0 としても完全に防ぐことができませんでした）

※ 以前は Google Chrome に最小フォントサイズの制限があり、16px フォントに font-size: 50% を適用しても 8px になりませんでした。
　 最低 10pxになってしまいました。現在は Google Chrome で最小フォントサイズが撤廃されたため、
　 半角スペースを全角サイズで用意して縮小することで、二分アキ、四分アキにすることができます。


FontForge で catalpa.sfd を開き、woff2 としてフォントを出力します。

woff2 を base64 に変換します。
Windows の場合は certutil -f -encode catalpa.woff2 catalpa.woff2.base64 のようにして base64 に変換できます。
その後、catalpa.woff2.base64 をテキストエディターで開き、-----BEGIN CERTIFICATE-----、-----END CERTIFICATE-----、および改行を削除します。

この woff2 を base64 にした文字列を css で @font-face として指定します。（フォントを別ファイルにせず、css に埋め込むことができます。）

@font-face {
	font-family: "catalpa";
	src: url("data:application/font-woff;base64,d09GMgABAAAAAAJwAA4AAAAABfQAAAIaAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP0ZGVE0cGhwGYACCQggEEQgKWHYLCgABNgIkAxAEIAWEBAcqGxMFAB4HdsNXE7bcDNNY/sXD/+/3bZ/77hPcH2p/SGOeEKtioZBYEyIa8RA1TchAVMtEGmt9eIL7+W6BAz9dgldY848oG/hKtHnqJv8inAD231y6/TFMw1QDDEso6hKKKCAJhIJwV/nyYIO5h+Qh2YkQ8OP968Pwo/YG/vQOvRKA+ZbLSAgEciPumlL2lPcRG+WbFh9cTUtJyjkAAgCOXz3a/Ki2rrqV2fIMEABoBQoUWC9Av0JgvaxFlgXablf6PdP9I4CoCEAkJABAUMguNV4uQXS7EsigQUKDhYCMAxKQ87yZRdcvzubMHzc90yrPfBm7994GXoSD/S/r7G/nRVV+7X9ZJH87XhRNsFQdRlX8738Z8cmBxNvvvU/OLer/18xrrLMvMY50uTwDjQSC8gzMNiBdJgCA3DAOGjQQyBoJJITAsA1goacE0BolINSuEZDUbhBQ2OabAFkdSwUozYuOAJU6jhqLPAfD0GlY6BJIZIYWpv1ptjASLa2MtWEV3mDQmExuUAp4fDW1yyVzrY5DOo7YpzXt6uHtbutrS500yODdZ3Vp/heWfdPXx/u+/W715JDBJYaaIZYz4P3cltyAkxyeRsxQ4imzKUzKpGrZlDwOjnzAIa1e1yZi1bu49X6HJQ89ch9TpZXSc7Rz4FDb286utaFzaP/hwgKeiQjOmlIlQPIwUruqZXaZ8ccT0O1xFQIAQD4OAQAA") format("woff2");
}

（例）
　<div class="content markdown"><!--start-search-target--><@markdown>${content!}</@markdown><!--end-search-target--></div>

