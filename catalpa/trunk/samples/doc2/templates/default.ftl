<!DOCTYPE html>
<html lang="ja">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<link rel="stylesheet" href="${baseurl}css/main.css">
	<link rel="stylesheet" href="${baseurl}lib/jsOnlyLightbox/css/lightbox.min.css">
	<link rel="icon" href="${baseurl}favicon.ico">
	<title>${title!}</title>
	<meta name="description" content="${description!}">
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
			</div>
		</div>
		<div class="flex-container">
			<div class="flex-item-left" id="flex-item-left">

				<div class="sidebar">
					<div class="sidebar-fixed-top px-3 pt-3 pb-1">
						<form method="GET" action="${baseurl}search.html?"
							onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
							<input id="search-keyword" type="search" name="keyword" placeholder="検索">
						</form>
					</div><@markdown replace_backslash_to_yensign=true>${sidebar!}</@markdown>
				</div>

			</div>
			<div class="flex-item-right">
				<div class="content markdown"><!--start-search-target--><@markdown replace_backslash_to_yensign=true>${content!}</@markdown><!--end-search-target--></div>
				<hr>
				<footer>
					<#if dateModified??>
					<span>最終更新日</span>
					<span class="datetime">${dateModified?string["yyyy-MM-dd"]}</span>
					</#if>
					<#if mailto?has_content>
					&thinsp;
					<address><a href="mailto:${mailto}"><#if author?has_content>${author}<#else>${mailto}</#if></a></address>
					</#if>
				</footer>
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

</body>
</html>
