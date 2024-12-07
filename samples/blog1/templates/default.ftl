<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<link rel="canonical" href="${url?remove_ending('index.html')}">
	<link rel="icon" href="${baseurl}favicon.ico">
	<title>${title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">

	<!-- OGP -->
	<meta property="og:site_name" content="${title!}">
	<meta property="og:type" content="website">
	<meta property="og:url" content="${url!}">
	<meta property="og:title" content="${title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<#if image?has_content><meta property="og:image" content="${siteurl!}/${image}"></#if>

	<!-- Twitter card -->
	<meta name="twitter:card" content="summary_large_image" />
	<meta name="twitter:title" content="${title!}" />
	<meta name="twitter:description" content="${(description!)?replace('\n', '')}" />
	<#if image?has_content><meta name="twitter:image" content="${siteurl!}/${image}"></#if>

	<!-- Web Fonts  Noto Sans JP, Noto Serif JP, Noto Sans Mono -->
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link href="https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@100..900&family=Noto+Serif+JP:wght@200..900&family=Noto+Sans+Mono:wdth,wght@62.5,100..900&display=swap" rel="stylesheet">

	<style><@compress single_line=true>
		<#include "css/main.css">
		<#include "css/markdown.css">
		<#include "css/highlight.css">
		${css!}
	</@compress></style>
</head>
<body>
	<#-- header -->
	<header>
		<#if !(hide!)?contains('header')>
		<div class="default">
			<div class="content">
				<#if (_PREVIEW!false) == true>
				<a class="title" href="/">
				<#else>
				<a class="title" href="${siteurl!}">
				</#if>
				<#if config.title?has_content>
				${config.title}
				<#else>
				${title!}
				</#if>
				</a>
				<form class="search" method="GET" action="${baseurl}search.html?"
					onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
					<input id="search-keyword" type="search" name="keyword" placeholder="検索">
				</form>
			</div>
		</div>
		</#if>
		<#if header?has_content>
		<div class="markdown"><@markdown  replace_backslash_to_yensign=true use_ruby=true use_catalpa_font=true>${header!}</@markdown></div>
		</#if>
	</header>

	<#-- main -->
	<main>
		<div class="content markdown"><!--start-search-target--><@markdown replace_backslash_to_yensign=true use_ruby=true use_catalpa_font=true>${content!}</@markdown><!--end-search-target--></div>
	</main>

	<#-- footer -->
	<footer>
		<#if footer?has_content>
		<div class="markdown"><@markdown  replace_backslash_to_yensign=true use_ruby=true use_catalpa_font=true>${footer!}</@markdown></div>
		</#if>

		<div class="default">
			<div class="content">
				<#-- SNS はてなブックマーク -->
				<a class="sns button hatena"
					href="https://b.hatena.ne.jp/add?mode=confirm&url=${url?remove_ending('index.html')}&title=${title!}" target="_blank"
					alt="はてなブックマーク"
					title="はてなブックマークに追加する"
					style="font-family:'Yu Gothic Medium','Yu Gothic','YuGothic';font-weight:bold;font-size:14px;line-height:1;margin:0;padding:6px 8px;border-radius:0.25em;color:white;background-color:#00A4DE">
					<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
						<g transform="matrix(34.1982,0,0,34.1982,-638.92,-838.889)">
							<path d="M50.076,46.458C48.309,44.572 45.859,43.469 43.276,43.398C45.429,42.948 47.398,41.864 48.928,40.284C50.173,38.721 50.802,36.753 50.692,34.758C50.721,33.057 50.31,31.376 49.5,29.88C48.682,28.464 47.485,27.305 46.044,26.532C44.57,25.755 42.976,25.231 41.328,24.984C38.12,24.615 34.89,24.471 31.662,24.552L20.448,24.552L20.448,65.452L32,65.452C35.355,65.52 38.71,65.364 42.044,64.984C43.837,64.739 45.576,64.198 47.192,63.384C48.859,62.52 50.232,61.179 51.134,59.532C52.084,57.757 52.562,55.767 52.52,53.754C52.66,51.101 51.786,48.491 50.076,46.458ZM30.816,33.606L33.21,33.606C35.982,33.606 37.842,33.918 38.79,34.542C39.8,35.296 40.341,36.528 40.212,37.782C40.334,39.029 39.741,40.244 38.682,40.914C37.674,41.526 35.782,41.814 33.03,41.814L30.816,41.814L30.816,33.606ZM40.316,57.06C39.216,57.726 37.346,58.05 34.716,58.05L30.816,58.05L30.816,49.14L34.884,49.14C37.584,49.14 39.456,49.482 40.446,50.166C41.53,51.029 42.101,52.388 41.958,53.766C42.119,55.1 41.465,56.408 40.3,57.078L40.316,57.06Z" style="fill:white;fill-rule:nonzero;"/>
						</g>
						<g transform="matrix(34.1982,0,0,34.1982,-638.92,-838.889)">
							<path d="M64.368,55.1C61.524,55.1 59.184,57.44 59.184,60.284C59.184,63.128 61.524,65.468 64.368,65.468C67.212,65.468 69.552,63.128 69.552,60.284C69.552,57.44 67.212,55.1 64.368,55.1Z" style="fill:white;fill-rule:nonzero;"/>
						</g>
						<g transform="matrix(34.1982,0,0,34.1982,-638.92,-838.889)">
							<rect x="59.868" y="24.552" width="9" height="27.274" style="fill:white;"/>
						</g>
					</svg>
					ブックマーク
				</a>
				<#-- SNS Facebook -->
				<a class="sns button facebook"
					href="https://www.facebook.com/share.php?u=${url?remove_ending('index.html')}" target="_blank"
					alt="Facebookシェア"
					title="Facebookでシェアする"
					style="font-family:'Yu Gothic Medium','Yu Gothic','YuGothic';font-weight:bold;font-size:87.5%;line-height:1;margin:0;padding:6px 8px;border-radius:0.25em;color:white;background-color:#1877F2">
					<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
						<g transform="matrix(1.37554,0,0,1.37554,195.722,-0.302579)">
							<path d="M1024,512.22C1024,229.45 794.77,0.22 512,0.22C229.23,0.22 0,229.45 0,512.22C0,767.774 187.231,979.59 432,1018L432,660.22L302,660.22L302,512.22L432,512.22L432,399.42C432,271.1 508.438,200.22 625.39,200.22C681.407,200.22 740,210.22 740,210.22L740,336.22L675.438,336.22C611.835,336.22 592,375.687 592,416.177L592,512.22L734,512.22L711.3,660.22L592,660.22L592,1018C836.769,979.59 1024,767.774 1024,512.22Z" style="fill:white;fill-rule:nonzero;"/>
						</g>
					</svg>
					シェア
				</a>
				<#-- SNS Twitter -->
				<a class="sns button twitter"
					href="https://twitter.com/intent/tweet?url=${url?remove_ending('index.html')}&text=${title!}" target="_blank"
					alt="Twitter"
					title="ツイートする"
					style="font-family:'Yu Gothic Medium','Yu Gothic','YuGothic';font-weight:bold;font-size:87.5%;line-height:1;margin:0;padding:6px 8px;border-radius:0.25em;color:white;background-color:#1D9BF0">
					<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
						<g id="twitter" transform="matrix(6.99956,0,0,6.99956,31.5298,-10.481)">
							<path d="M221.95,51.29C222.1,53.46 222.1,55.63 222.1,57.82C222.1,124.55 171.3,201.51 78.41,201.51L78.41,201.47C50.97,201.51 24.1,193.65 1,178.83C4.99,179.31 9,179.55 13.02,179.56C35.76,179.58 57.85,171.95 75.74,157.9C54.13,157.49 35.18,143.4 28.56,122.83C36.13,124.29 43.93,123.99 51.36,121.96C27.8,117.2 10.85,96.5 10.85,72.46L10.85,71.82C17.87,75.73 25.73,77.9 33.77,78.14C11.58,63.31 4.74,33.79 18.14,10.71C43.78,42.26 81.61,61.44 122.22,63.47C118.15,45.93 123.71,27.55 136.83,15.22C157.17,-3.9 189.16,-2.92 208.28,17.41C219.59,15.18 230.43,11.03 240.35,5.15C236.58,16.84 228.69,26.77 218.15,33.08C228.16,31.9 237.94,29.22 247.15,25.13C240.37,35.29 231.83,44.14 221.95,51.29Z" style="fill:white;fill-rule:nonzero;"/>
						</g>
					</svg>
					ツイート
				</a>
				<#-- COPYRIGHT -->
				<span class="copyright">
					${copyright!}&ensp;
					<#if mailto?has_content><address><a href="mailto:${mailto}"></#if>
					<#if author?has_content>${author}<#else>${mailto!}</#if>
					<#if mailto?has_content></a></address></#if>
				</span>
			</div>
		</div>
	</footer>

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
		const sticky_header = document.getElementById("sticky-header");
		if (sticky_header != null) {
			const resizeObserver = new ResizeObserver((entries) => {
				if (sticky_header != null) {
					document.documentElement.style.scrollPaddingTop = (sticky_header.clientHeight - 4) + "px";
				}
			});
			resizeObserver.observe(document.body);
		}
	</script>

	<#if (_PREVIEW!false) == true>
	<script>
		function waitForUpdate() {
			var xhr = new XMLHttpRequest();
			xhr.onload = function (e) {
				if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						location.reload();
						return;
					}
				}
				waitForUpdate();
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
