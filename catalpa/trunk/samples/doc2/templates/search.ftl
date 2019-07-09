<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<meta name="robots" content="noindex">
	<link rel="stylesheet" href="${baseurl}css/main.css">
	<link rel="stylesheet" href="${baseurl}lib/jsOnlyLightbox/css/lightbox.min.css">
	<link rel="icon" href="${baseurl}favicon.ico">
	<title>${title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">

	<!-- OGP -->
	<meta property="og:site_name" content="${title!}">
	<meta property="og:type" content="website">
	<meta property="og:url" content="${siteurl!}">
	<meta property="og:title" content="${title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<!-- <meta property="og:image" content=""> -->
</head>
<body>
	<div class="body-center">
		<div class="header" id="header">
			<div class="header-title">
				&nbsp;
				<#if (_PREVIEW!false) == true>
				<a href="/"><#if icon?has_content><img class="icon" src="${baseurl}${icon}">&nbsp;</#if>${config.title!}</a>
				<#else>
				<#if siteurl?has_content><a href="${siteurl}"></#if><#if icon?has_content><img class="icon" src="${baseurl}${icon}">&nbsp;</#if>${config.title!}<#if siteurl?has_content></a></#if>
				</#if>
				<form style="float:right" onsubmit="var e = document.getElementById('search-keyword'); search(e.value); e.select(); return false;">
					<input id="search-keyword" type="search" name="keyword" placeholder="検索">
				</form>
			</div>
		</div>
		<div class="flex-container">
			<div class="flex-item-left" id="flex-item-left">
				<div class="sidebar"><@markdown replace_backslash_to_yensign=true>${sidebar!}</@markdown></div>
			</div>
			<div class="flex-item-right">
				<div id="search-result" class="search-result"></div>
			</div>
		</div>
	</div>
	<div class="body-left">
		<div class="body-left-adv"></div>
	</div>
	<div class="body-right">
		<div class="body-right-adv"></div>
	</div>

	<script>
		function onResize() {
			var header = document.getElementById("header");
			if(header) {
				var e = document.getElementById("flex-item-left");
				e.style.height = "auto";
				if(e) {
					var viewportHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
					if(header.clientHeight + e.clientHeight >= viewportHeight) {
						e.style.top = "calc(-" + e.clientHeight + "px + 100vh)";
					} else {
						e.style.top = header.clientHeight + "px";
						e.style.height = (viewportHeight - header.clientHeight) + "px";
					}
				}
			}
		}
		window.addEventListener('resize', function (event) {
			onResize();
		});
		onResize();
	</script>

	<script src="${baseurl}lib/jsOnlyLightbox/js/lightbox.min.js"></script>
	<script>
		var e = document.getElementsByClassName("zoom");
		for(var i = 0; i < e.length; i++) {
			if(e[i].tagName == "IMG") {
				e[i].className += " jslghtbx-thmb";
				e[i].setAttribute("data-jslghtbx", "");
				e[i].setAttribute("data-jslghtbx-group", "default");
				e[i].setAttribute("title", "クリックすると拡大します");
			}
		}
		var lightbox = new Lightbox();
		var lightboxOptions = {
			nextOnClick: false,
			hideOverflow: false,
			animation: 1
		}
		lightbox.load(lightboxOptions);
	</script>

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

			keyword = keyword.replace("%20", " ").trim();
			if(keyword.length == 0) {
				result.innerHTML = "<p class=\"markdown\">検索したい文字列を入力して <kbd>Enter</kbd></span> キーを押してください。</p>";
				return;
			}

			var startTime = new Date();
			var arr = keyword.split(" ");
			var keywords_AND = "";
			var keywords_OR = "";
			for(var i = 0; i < arr.length; i++) {
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
			if(keywords_AND.length == 0) {
				return;
			}
			var regexp_AND = new RegExp(keywords_AND, "i");
			var regexp_OR = new RegExp(keywords_OR.substring(1), "ig");
			var regexp_STRONG = new RegExp(htmlTagEscape(keywords_OR.substring(1)), "ig");

			var matches = [];
			for(var i = 0; i < db.length; i++) {
				var entry = db[i];
				if(entry.text.match(regexp_AND)) {
					matches[matches.length] = i;
				}
			}

			if(matches.length == 0) {
				result.innerHTML = "<p><strong>" + keyword + "</strong> に一致する情報は見つかりませんでした。</p>";
				return;
			}

			var entries = "";
			for(var i = 0; i < matches.length; i++) {
				var db_entry = db[matches[i]];
				var entry = "<div class=\"entry\">"
					+ "<div class=\"title\"><a href=\"" + urlPrefix + db_entry.url + "\">" + htmlTagEscape(db_entry.title) + "</a></div>"
					+ "<div class=\"url\"><a href=\"" + urlPrefix + db_entry.url + "\">" + decodeURI(urlPrefix + db_entry.url) + "</a></div>";
				var text = "";
				var divider = "";
				var lines = db_entry.text.split("\n");
				for(var j = 0; j < lines.length; j++) {
					var range_index_s;
					var range_index_e = -1;
					var candidate = "";
					var r;
					while(r = regexp_OR.exec(lines[j])) {
						var index_s = Math.max(0, r.index - 25);
						var index_e = Math.min(lines[j].length, r.index + r[0].length + 25);
						if(index_s < range_index_e) {
							range_index_e = index_e;
						} else {
							if(candidate.length > 0) {
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
					if(candidate.length > 0) {
						text += divider + (range_index_s > 0 ? "&hellip;" : "")
							 + candidate.replace(regexp_STRONG, "<strong>$&</strong>")
							 + (range_index_e < lines[j].length ? "&hellip;" : "");
						candidate = "";
						divider = "<span class=\"divider\"></span>";
					}
					if(text.length > 400) {
						break;
					}
				}
				if(text.length > 0) {
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
		if(location.search.indexOf("?keyword=") === 0) {
			var keyword = decodeURIComponent(location.search.substring(9).replace(/\+/g, " "));
		}
		search(keyword);
			
		var input = document.getElementById('search-keyword');
		input.value = keyword;
		input.focus();
		input.select();
	</script>

</body>
</html>
