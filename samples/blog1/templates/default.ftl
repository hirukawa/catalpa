<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
${head!}
	<meta charset="utf-8">
	<#if robots?has_content><meta name="robots" content="${robots}"></#if>
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<link rel="canonical" href="${url?remove_ending('index.html')}">
	<#if icon?has_content><link rel="icon" href="<#if !icon?starts_with("/")>${baseurl}</#if>${icon}"></#if>
	<title>${title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">
	<#if theme_color?has_content><meta name="theme-color" content="${theme_color}"></#if>

	<!-- OGP -->
	<meta property="og:site_name" content="${title!}">
	<meta property="og:type" content="website">
	<meta property="og:url" content="${url!}">
	<meta property="og:title" content="${title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<#if image?has_content><meta property="og:image" content="${siteurl!}/${image}"></#if>

	<!-- X (Twitter) card -->
	<meta name="twitter:card" content="summary_large_image" />

	<#include "templates/webfont" ignore_missing=true>

	<style><@compress single_line=true>
		<#include "css/system.css" parse=false>
		<#include "css/color.css" parse=false>
		<#include "css/main.css" parse=false>
		<#include "css/markdown.css" parse=false>
		<#include "css/highlight.css" parse=false>
		<#include "css/custom.css" parse=false ignore_missing=true>
		${css!}
	</@compress></style>
</head>
<body>
	<#-- header -->
	<header>
		<#if !config.hide!?contains('header') && !hide!?contains('header')>
		<div class="default">
			<div class="content">
				<#if (_PREVIEW!false) == true>
				<a class="title" href="${title_url!?ensure_starts_with("/")}">
				<#else>
				<a class="title" href="${siteurl!}${title_url!?ensure_starts_with("/")}">
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
		<div class="markdown"><@markdown>${header!}</@markdown></div>
		</#if>
	</header>

	<#-- main -->
	<main>
		<div class="content markdown"><!--start-search-target--><@markdown>${content!}</@markdown><!--end-search-target--></div>
	</main>

	<#-- footer -->
	<footer>
		<#if footer?has_content>
		<div class="markdown"><@markdown>${footer!}</@markdown></div>
		</#if>

		<#if !config.hide!?contains('footer') && !hide!?contains('footer')>
		<div class="default">
			<div class="content">
				<#-- SNS -->
				<#if !config.hide!?contains('sns_all') && !hide!?contains('sns_all')>
				<#-- SNS はてなブックマーク -->
				<#if !config.hide!?contains('sns_hatena') && !hide!?contains('sns_hatena')>
				<a class="sns button hatena"
					href="https://b.hatena.ne.jp/add?mode=confirm&url=${url?remove_ending('index.html')}&title=${title!}" target="_blank"
					alt="はてなブックマーク"
					title="はてなブックマークに追加する"
					style="font-family:var(--font-family-sans-serif);font-weight:500;font-size:14px;line-height:1;margin:0;padding:6px 8px;border-radius:4px;color:white;background-color:#00A4DE">
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
				</#if>
				<#-- SNS Facebook -->
				<#if !config.hide!?contains('sns_facebook') && !hide!?contains('sns_facebook')>
				<a class="sns button facebook"
					href="https://www.facebook.com/share.php?u=${url?remove_ending('index.html')}" target="_blank"
					alt="Facebookシェア"
					title="Facebookでシェアする"
					style="font-family:var(--font-family-sans-serif);font-weight:500;font-size:14px;line-height:1;margin:0;padding:6px 8px;border-radius:4px;color:white;background-color:#1877F2">
					<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
						<g transform="matrix(1.37554,0,0,1.37554,195.722,-0.302579)">
							<path d="M1024,512.22C1024,229.45 794.77,0.22 512,0.22C229.23,0.22 0,229.45 0,512.22C0,767.774 187.231,979.59 432,1018L432,660.22L302,660.22L302,512.22L432,512.22L432,399.42C432,271.1 508.438,200.22 625.39,200.22C681.407,200.22 740,210.22 740,210.22L740,336.22L675.438,336.22C611.835,336.22 592,375.687 592,416.177L592,512.22L734,512.22L711.3,660.22L592,660.22L592,1018C836.769,979.59 1024,767.774 1024,512.22Z" style="fill:white;fill-rule:nonzero;"/>
						</g>
					</svg>
					シェア
				</a>
				</#if>
				<#-- SNS X (Twitter) -->
				<#if !config.hide!?contains('sns_x') && !hide!?contains('sns_x')>
				<a class="sns button x"
					href="https://x.com/intent/post?url=${url?remove_ending('index.html')}&text=${title!}" target="_blank"
					alt="X (Twitter)"
					title="Xにポストする"
					style="font-family:var(--font-family-sans-serif);font-weight:500;font-size:14px;line-height:1;margin:0;padding:6px 8px;border-radius:4px;color:white;background-color:#000000">
					<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
						<path d="M1030.26,592.5l509.713,-592.5l-120.785,0l-442.584,514.459l-353.49,-514.459l-407.71,0l534.547,777.954l-534.547,621.327l120.793,0l467.38,-543.287l373.311,543.287l407.71,0l-554.367,-806.781l0.029,0Zm-165.442,192.308l-54.16,-77.467l-430.938,-616.41l185.53,0l347.772,497.463l54.161,77.466l452.062,646.626l-185.531,0l-368.896,-527.649l0,-0.029Z" style="fill:white;fill-rule:nonzero;"/>
					</svg>
					ポスト
				</a>
				</#if>
				</#if>

				<#-- LEGAL -->
				<#if legal?has_content>
				<div class="markdown legal"><@markdown>${legal!}</@markdown></div>
				</#if>

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

		document.addEventListener("copy", (event) => {
			const selection = document.getSelection();
			if (selection.type == "Range") {
				var s = selection.toString();
				s = s.replaceAll(/ ([・：；]) /g, "$1");
				s = s.replaceAll(/ ([“‘（〔［｛〈《「『【])/g, "$1");
				s = s.replaceAll(/([”’）〕］｝〉》」』】、，]) /g, "$1");
				event.clipboardData.setData("text/plain", s);
				event.preventDefault();
			}
		});
	</script>

	<#if (_PREVIEW!false) == true>
	<script>
		var xhr = new XMLHttpRequest();

		function waitForUpdate() {
			xhr.open("GET", "/wait-for-update?random=" + Math.random(), true);
			xhr.send(null);
		}

		xhr.onload = function (e) {
			if (xhr.status === 205 && location.pathname !== "/" && location.pathname !== "/index.html") {
				location.href = "/";
			} else if (xhr.status === 200 || xhr.status === 205) {
				location.reload();
			} else if (xhr.status === 429) {
				// Too Many Requests
			} else {
				waitForUpdate();
			}
		};
		xhr.ontimeout = function (e) {
			waitForUpdate();
		};
		xhr.onerror = function (e) {
			waitForUpdate();
		};
		window.onbeforeunload = function (e) {
			xhr.abort();
		};

		waitForUpdate();
	</script>
	</#if>

${tail!}
</body>
</html>
