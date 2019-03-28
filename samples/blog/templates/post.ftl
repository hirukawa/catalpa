<!DOCTYPE html>
<html lang="ja">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<link rel="stylesheet" href="${baseurl}css/main.css">
	<link rel="stylesheet" href="${baseurl}lib/jsOnlyLightbox/css/lightbox.min.css">
	<link rel="icon" href="${baseurl}favicon.ico">
	<title>${blog.title!}</title>
	<meta name="description" content="${description!}">
</head>
<body class="blog post">
	<div class="body-left">
		<div class="body-left-adv"></div>
	</div>
	<div class="body-center">
		<div class="header" id="header">
			<div class="header-title">
				&nbsp;
				<#if (_PREVIEW!false) == true>
				<a href="/">${blog.title!}</a>
				<#else>
				<#if siteurl?has_content><a href="${siteurl}"></#if>${blog.title!}<#if siteurl?has_content></a></#if>
				</#if>
			</div>
		</div>
		<div class="flex-container">
			<div class="flex-item-left" id="flex-item-left">
				<#include "sidebar.ftl">
			</div>
			<div class="flex-item-right">
				<article class="post">
					<header>
						<div class="title">${blog.post.title}</div>
						<div>
							<span class="datetime">${blog.post.date}</span>
							<span class="categories">
							<#list blog.post.categories as category>
								<a class="category" href="${baseurl}${category.url}">${category.name}</a>
							</#list>
							</span>
						</div>
					</header>
					<div class="content markdown"><@markdown replace_backslash_to_yensign=true>${content!}</@markdown></div>
				</article>

				<#-- footer pager -->
				<ul class="pager <#if blog.pager.previous??>left</#if> <#if blog.pager.next??>right</#if>">
				<#if blog.pager.previous??>
					<li class="left">
						<a href="${blog.pager.previous.url}" class="previous"><span class="datetime">${blog.pager.previous.date}</span> ${blog.pager.previous.title}</a>
					</li>
				</#if>
				<#if blog.pager.next??>
					<li class="right">
						<a href="${blog.pager.next.url}" class="next"><span class="datetime">${blog.pager.next.date}</span> ${blog.pager.next.title}</a>
					</li>
				</#if>
				</ul>
			</div>
		</div>
	</div>
	<div class="body-right">
		<div class="body-right-adv"></div>
	</div>

	<script type="text/javascript">
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

	<script src="${baseurl}lib/jsOnlyLightbox/js/lightbox.min.js" type="text/javascript"></script>
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
	<script type="text/javascript">
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
