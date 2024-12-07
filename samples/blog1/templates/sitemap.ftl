<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
<#list sitemap as item>
	<url>
		<loc>${item.loc?remove_ending('index.html')}</loc>
		<lastmod>${item.lastmod_iso8601}</lastmod>
		<changefreq>${item.changefreq}</changefreq>
		<priority>${item.priority}</priority>
	</url>
</#list>
</urlset>
