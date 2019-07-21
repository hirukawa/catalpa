<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">

	<#if blog.pager.next??>
	<meta name="robots" content="noindex">
	<#elseif siteurl?has_content>
	<link rel="canonical" href="${siteurl}">
	</#if>

	<link rel="stylesheet" href="${baseurl}css/main.css">
	<link rel="stylesheet" href="${baseurl}lib/jsOnlyLightbox/css/lightbox.min.css">
	<link rel="icon" href="${baseurl}favicon.ico">
	<title>${blog.title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">

	<!-- OGP -->
	<meta property="og:site_name" content="${blog.title!}">
	<meta property="og:type" content="website">
	<meta property="og:url" content="${siteurl!}">
	<meta property="og:title" content="${blog.title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<#if image?has_content><meta property="og:image" content="${siteurl!}/${image}"></#if>
</head>
<body class="blog page">
	<div class="body-center">
		<div class="header" id="header">
			<div class="header-title">
				&nbsp;
				<#if (_PREVIEW!false) == true>
				<a href="/"><#if icon?has_content><img class="icon" src="${baseurl}${icon}">&nbsp;</#if>${blog.title!}</a>
				<#else>
				<#if siteurl?has_content><a href="${siteurl}"></#if><#if icon?has_content><img class="icon" src="${baseurl}${icon}">&nbsp;</#if>${blog.title!}<#if siteurl?has_content></a></#if>
				</#if>
				<form style="float:right" method="GET" action="${baseurl}search.html?"
					onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
					<input id="search-keyword" type="search" name="keyword" placeholder="検索">
				</form>
			</div>
		</div>
		<div class="flex-container">
			<div class="flex-item-left" id="flex-item-left">
				<#include "sidebar.ftl">
			</div>
			<div class="flex-item-right">

				<#list blog.page.posts as post>
				<article class="card">
					<header>
						<a class="title" href="${baseurl}${post.url}">${post.title}</a>
						<div>
						 	<span class="datetime">${post.date}</span>
							<span class="categories">
							<#list post.categories as category>
								<a class="category" href="${baseurl}${category.url}">${category.name}</a>
							</#list>
							</span>
						</div>
					</header>
					<div class="leading markdown"><@markdown replace_backslash_to_yensign=true relative_url_prefix="${post.relativeUrlPrefix}">${post.leading}</@markdown></div>
					<footer>
						<a class="more" href="${baseurl}${post.url}">続きを読む...</a>
					</footer>
				</article>
				</#list>

				<#-- footer pager -->
				<ul class="pager <#if blog.pager.previous??>left</#if> <#if blog.pager.next??>right</#if>">
				<#if blog.pager.previous??>
					<li class="left">
						<a href="${blog.pager.previous.url}" class="previous"><span class="title">${blog.pager.previous.title}</span></a>
					</li>
				</#if>
				<#if blog.pager.next??>
					<li class="right">
						<a href="${blog.pager.next.url}" class="next"><span class="title">${blog.pager.next.title}</span></a>
					</li>
				</#if>
				</ul>

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
