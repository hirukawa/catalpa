<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
${head!}
	<meta charset="utf-8">
	<meta name="robots" content="noindex">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<#if icon?has_content><link rel="icon" href="<#if !icon?starts_with("/")>${baseurl}</#if>${icon}"></#if>
	<title>${title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">
	<#if theme_color?has_content><meta name="theme-color" content="${theme_color}"></#if>

	<#include "templates/webfont" ignore_missing=true>

	<style><@compress single_line=true>
		<#include "css/system.css">
		<#include "css/color.css">
		<#include "css/main.css">
		<#include "css/search.css">
		<#include "css/custom.css" ignore_missing=true>
		${css!}
	</@compress></style>
</head>
<body>
	<#-- search -->
	<header>
		<#if !config.hide!?contains('header') && !hide!?contains('header')>
		<div class="default">
			<div class="content">
				<#if (_PREVIEW!false) == true>
				<a class="title" href="${title_url!?ensure_starts_with("/")}">
				<#else>
				<a class="title" href="${siteurl!}${title_url!?ensure_starts_with("/")}">
				</#if>
				${title!}
				</a>
				<form class="search" onsubmit="var e = document.getElementById('search-keyword'); search(e.value); e.select(); return false;">
					<input id="search-keyword" type="search" name="keyword" placeholder="検索">
				</form>
			</div>
		</div>
		</#if>

		<#if header?has_content>
		<div class="markdown"><@markdown>${header!}</@markdown></div>
		</#if>
	</header>

	<#-- main -->
	<main>
		<div id="search-result" class="search-result content"></div>
	</main>

	<#-- footer -->
	<footer>
		<#if footer?has_content>
		<div class="markdown"><@markdown>${footer!}</@markdown></div>
		</#if>

		<#if !config.hide!?contains('footer') && !hide!?contains('footer')>
		<div class="default">
			<div class="content">
				<#-- COPYRIGHT -->
				<span class="copyright">
					${copyright!}&ensp;
					<#if mailto?has_content><address><a href="mailto:${mailto}"></#if>
					<#if author?has_content>${author}<#else>${mailto!}</#if>
					<#if mailto?has_content></a></address></#if>
				</span>
			</div>
		</div>
		</#if>
	</footer>

	<#if (_PREVIEW!false) == true>
	<script>
		function waitForUpdate() {
			var xhr = new XMLHttpRequest();
			xhr.onload = function (e) {
				if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						location.reload();
					}
					waitForUpdate();
				}
			};
			xhr.onerror = function (e) {
				waitForUpdate();
			};
			xhr.open("GET", "/wait-for-update", true);
			xhr.send(null);
		}
		waitForUpdate();
	</script>
	</#if>

	<script>
		var ua = window.navigator.userAgent.toLowerCase();
		if (ua.indexOf("windows") !== -1) {
			document.documentElement.style.setProperty("--is-windows", 1)
		} else if (ua.indexOf("mac os") !== -1) {
			document.documentElement.style.setProperty("--is-mac", 1);
		} else if (ua.indexOf("android") !== -1) {
			document.documentElement.style.setProperty("--is-android", 1);
		} else if (ua.indexOf("iphone") !== -1) {
			document.documentElement.style.setProperty("--is-iphone", 1);
		}

		var urlPrefix = location.href.substring(0, location.href.lastIndexOf("/") + 1);

		function htmlTagEscape(str) {
			if (!str) return;
			return str.replace(/[<>]/g, function(match) {
				const escape = {
					'<': '&lt;',
					'>': '&gt;'
				};
				return escape[match];
			});
		}

		function search(keyword) {
			history.replaceState({}, document.title, location.pathname + "?keyword=" + encodeURI(keyword));

			var result = document.getElementById("search-result");
			result.innerHTML = "";
			window.scrollTo(0, 0);

			keyword = keyword.replace("<", "&lt;").replace(">", "&gt;");
			keyword = keyword.replace("%20", " ").trim();
			if (keyword.length == 0) {
				result.innerHTML = "<p class=\"markdown\">検索したい文字列を入力して <kbd>Enter</kbd></span> キーを押してください。</p>";
				return;
			}

			var startTime = new Date();
			var arr = keyword.split(" ");
			var keywords_AND = "";
			var keywords_OR = "";
			for (var i = 0; i < arr.length; i++) {
				var s = arr[i];
				s = s.replace(/\\/g, "\\\\");
				s = s.replace(/\*/g, "\\*");
				s = s.replace(/\+/g, "\\+");
				s = s.replace(/\./g, "\\.");
				s = s.replace(/\?/g, "\\?");
				s = s.replace(/\{/g, "\\{");
				s = s.replace(/\}/g, "\\}");
				s = s.replace(/\(/g, "\\(");
				s = s.replace(/\)/g, "\\)");
				s = s.replace(/\[/g, "\\[");
				s = s.replace(/\]/g, "\\]");
				s = s.replace(/\^/g, "\\^");
				s = s.replace(/\$/g, "\\$");
				s = s.replace(/\-/g, "\\-");
				s = s.replace(/\|/g, "\\|");
				s = s.replace(/\//g, "\\/");
				keywords_AND += "(?=.*" + s + ")";
				keywords_OR += "|" + s;
			}
			if (keywords_AND.length == 0) {
				return;
			}
			var regexp_AND = new RegExp(keywords_AND, "i");
			var regexp_OR = new RegExp(keywords_OR.substring(1), "ig");
			var regexp_STRONG = new RegExp(htmlTagEscape(keywords_OR.substring(1)), "ig");

			var matches = [];
			for (var i = 0; i < db.length; i++) {
				var entry = db[i];
				if(entry.text.match(regexp_AND)) {
					matches[matches.length] = i;
				}
			}

			if (matches.length == 0) {
				result.innerHTML = "<p><strong>" + keyword + "</strong> に一致する情報は見つかりませんでした。</p>";
				return;
			}

			var entries = "";
			for (var i = 0; i < matches.length; i++) {
				var db_entry = db[matches[i]];
				var entry = "<div class=\"entry\">"
					+ "<div class=\"title\"><a href=\"" + urlPrefix + db_entry.url + "\">" + htmlTagEscape(db_entry.title) + "</a></div>"
					+ "<div class=\"url\"><a href=\"" + urlPrefix + db_entry.url + "\">" + decodeURI(urlPrefix + db_entry.url) + "</a></div>";
				var text = "";
				var divider = "";
				var lines = db_entry.text.split("\n");
				for (var j = 0; j < lines.length; j++) {
					var range_index_s;
					var range_index_e = -1;
					var candidate = "";
					var r;
					while (r = regexp_OR.exec(lines[j])) {
						var index_s = Math.max(0, r.index - 25);
						var index_e = Math.min(lines[j].length, r.index + r[0].length + 25);
						if (index_s < range_index_e) {
							range_index_e = index_e;
						} else {
							if (candidate.length > 0) {
								text += divider + (range_index_s > 0 ? "&hellip;" : "")
									 + candidate.replace(regexp_STRONG, "<strong>$&</strong>")
									 + (range_index_e < lines[j].length ? "&hellip;" : "");
								candidate = "";
								divider = "<span class=\"divider\"></span>";
								if(text.length > 400) {
									break;
								}
							}
							range_index_s = index_s;
							range_index_e = index_e;
						}
						candidate = htmlTagEscape(lines[j].substring(range_index_s, range_index_e));
					}
					if (candidate.length > 0) {
						text += divider + (range_index_s > 0 ? "&hellip;" : "")
							 + candidate.replace(regexp_STRONG, "<strong>$&</strong>")
							 + (range_index_e < lines[j].length ? "&hellip;" : "");
						candidate = "";
						divider = "<span class=\"divider\"></span>";
					}
					if (text.length > 400) {
						break;
					}
				}
				if (text.length > 0) {
					entry += "<div class=\"text\">" + text + "</div>";
				}
				entries += entry + "</div>";
			}
			var endTime = new Date();
			result.innerHTML = ""
				+ "<div class=\"search-result-header\">"
				+ "    <span class=\"count\">" + matches.length + " 件</span> "
				+ "	   <span class=\"time\">（" + (endTime - startTime) + "ミリ秒）</span>"
				+ "</div>"
				+ "<div class=\"entries\">" + entries + "</div>";
		}

		var db = [${db}];

		var keyword = "";
		if (location.search.indexOf("?keyword=") === 0) {
			var keyword = decodeURIComponent(location.search.substring(9).replace(/\+/g, " "));
		}
		search(keyword);
			
		var input = document.getElementById('search-keyword');
		input.value = keyword;
		input.focus();
		input.select();
	</script>

${tail!}
</body>
</html>
