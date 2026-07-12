package com.furat.browser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

class AdBlocker {

    // Common ad/tracker domains
    private val adDomains = setOf(
        "googleadservices.com", "googlesyndication.com", "google-analytics.com",
        "doubleclick.net", "adservice.google.com", "googletagmanager.com",
        "facebook.com/tr", "connect.facebook.net", "analytics.facebook.com",
        "ads.yahoo.com", "advertising.yahoo.com", "gemini.yahoo.com",
        "amazon-adsystem.com", "s.amazon-adsystem.com",
        "adsystem.amazon.com", "c.amazon-adsystem.com",
        "ads.twitter.com", "analytics.twitter.com",
        "ads.linkedin.com", "analytics.linkedin.com",
        "ads.reddit.com", "analytics.reddit.com",
        "ads.tiktok.com", "analytics.tiktok.com",
        "ads.microsoft.com", "analytics.microsoft.com",
        "ads.apple.com", "analytics.apple.com",
        "ads.snapchat.com", "analytics.snapchat.com",
        "ads.pinterest.com", "analytics.pinterest.com",
        "ads.instagram.com", "analytics.instagram.com",
        "ads.whatsapp.com", "analytics.whatsapp.com",
        "ads.youtube.com", "analytics.youtube.com",
        "ads.spotify.com", "analytics.spotify.com",
        "ads.netflix.com", "analytics.netflix.com",
        "ads.adobe.com", "analytics.adobe.com",
        "ads.outbrain.com", "analytics.outbrain.com",
        "ads.taboola.com", "analytics.taboola.com",
        "ads.revcontent.com", "analytics.revcontent.com",
        "ads.mgid.com", "analytics.mgid.com",
        "ads.adform.net", "analytics.adform.net",
        "ads.criteo.com", "analytics.criteo.com",
        "ads.rubiconproject.com", "analytics.rubiconproject.com",
        "ads.openx.net", "analytics.openx.net",
        "ads.pubmatic.com", "analytics.pubmatic.com",
        "ads.appnexus.com", "analytics.appnexus.com",
        "ads.indexexchange.com", "analytics.indexexchange.com",
        "ads.sovrn.com", "analytics.sovrn.com",
        "ads.teads.tv", "analytics.teads.tv",
        "ads.adzerk.net", "analytics.adzerk.net",
        "ads.adroll.com", "analytics.adroll.com",
        "ads.retargeter.com", "analytics.retargeter.com",
        "ads.perfectaudience.com", "analytics.perfectaudience.com",
        "ads.adblade.com", "analytics.adblade.com",
        "ads.adsterra.com", "analytics.adsterra.com",
        "ads.popads.net", "analytics.popads.net",
        "ads.popcash.net", "analytics.popcash.net",
        "ads.propellerads.com", "analytics.propellerads.com",
        "ads.adcash.com", "analytics.adcash.com",
        "ads.exoclick.com", "analytics.exoclick.com",
        "ads.juicyads.com", "analytics.juicyads.com",
        "ads.bidvertiser.com", "analytics.bidvertiser.com",
        "ads.clicksor.com", "analytics.clicksor.com",
        "ads.chitika.com", "analytics.chitika.com",
        "ads.infolinks.com", "analytics.infolinks.com",
        "ads.kontera.com", "analytics.kontera.com",
        "ads.vibrantmedia.com", "analytics.vibrantmedia.com",
        "ads.intellitxt.com", "analytics.intellitxt.com",
        "ads.snap.com", "analytics.snap.com",
        "ads.quantserve.com", "analytics.quantserve.com",
        "ads.scorecardresearch.com", "analytics.scorecardresearch.com",
        "ads.comscore.com", "analytics.comscore.com",
        "ads.nielsen.com", "analytics.nielsen.com",
        "ads.moatads.com", "analytics.moatads.com",
        "ads.iasds.net", "analytics.iasds.net",
        "ads.doubleverify.com", "analytics.doubleverify.com",
        "ads.integralads.com", "analytics.integralads.com",
        "ads.forensiq.com", "analytics.forensiq.com",
        "ads.pixalate.com", "analytics.pixalate.com",
        "ads.whiteops.com", "analytics.whiteops.com",
        "ads.tamome.com", "analytics.tamome.com",
        "ads.adloox.com", "analytics.adloox.com",
        "ads.adsafeprotected.com", "analytics.adsafeprotected.com",
        "ads.geoedge.com", "analytics.geoedge.com",
        "ads.confiant.com", "analytics.confiant.com",
        "ads.themediatrust.com", "analytics.themediatrust.com",
        "ads.adlightning.com", "analytics.adlightning.com",
        "ads.clean.io", "analytics.clean.io",
        "ads.adomik.com", "analytics.adomik.com",
        "ads.adthrive.com", "analytics.adthrive.com",
        "ads.mediate.com", "analytics.mediate.com",
        "ads.adpushup.com", "analytics.adpushup.com",
        "ads.ezoic.com", "analytics.ezoic.com",
        "ads.monumetric.com", "analytics.monumetric.com",
        "ads.shemedia.com", "analytics.shemedia.com",
        "ads.freestar.com", "analytics.freestar.com",
        "ads.sortable.com", "analytics.sortable.com",
        "ads.headerlift.com", "analytics.headerlift.com",
        "ads.pubgalaxy.com", "analytics.pubgalaxy.com",
        "ads.adrecover.com", "analytics.adrecover.com",
        "ads.adblockanalytics.com", "analytics.adblockanalytics.com",
        "ads.adblockplus.org", "analytics.adblockplus.org",
        "ads.ublock.org", "analytics.ublock.org",
        "ads.easylist.to", "analytics.easylist.to",
        "ads.fanboy.co.nz", "analytics.fanboy.co.nz",
        "ads.adguard.com", "analytics.adguard.com",
        "ads.ghostery.com", "analytics.ghostery.com",
        "ads.privacybadger.org", "analytics.privacybadger.org",
        "ads.disconnect.me", "analytics.disconnect.me",
        "ads.ublockorigin.com", "analytics.ublockorigin.com",
        "ads.adnauseam.io", "analytics.adnauseam.io",
        "ads.blokada.org", "analytics.blokada.org",
        "ads.dns66.com", "analytics.dns66.com",
        "ads.adaway.org", "analytics.adaway.org",
        "ads.hosts-file.net", "analytics.hosts-file.net",
        "ads.someonewhocares.org", "analytics.someonewhocares.org",
        "ads.winhelp2002.mvps.org", "analytics.winhelp2002.mvps.org",
        "ads.pgl.yoyo.org", "analytics.pgl.yoyo.org",
        "ads.malwaredomainlist.com", "analytics.malwaredomainlist.com",
        "ads.phishing.army", "analytics.phishing.army",
        "ads.urlhaus.abuse.ch", "analytics.urlhaus.abuse.ch",
        "ads.threatfox.abuse.ch", "analytics.threatfox.abuse.ch",
        "ads.bazaar.abuse.ch", "analytics.bazaar.abuse.ch",
        "ads.malware.bazaar", "analytics.malware.bazaar",
        "ads.virustotal.com", "analytics.virustotal.com",
        "ads.hybrid-analysis.com", "analytics.hybrid-analysis.com",
        "ads.any.run", "analytics.any.run",
        "ads.joesandbox.com", "analytics.joesandbox.com",
        "ads.cuckoo.sh", "analytics.cuckoo.sh",
        "ads.malwr.com", "analytics.malwr.com",
        "ads.virscan.org", "analytics.virscan.org",
        "ads.metadefender.com", "analytics.metadefender.com",
        "ads.opswat.com", "analytics.opswat.com",
        "ads.reversinglabs.com", "analytics.reversinglabs.com",
        "ads.lastline.com", "analytics.lastline.com",
        "ads.vmray.com", "analytics.vmray.com",
        "ads.hatching.io", "analytics.hatching.io",
        "ads.tria.ge", "analytics.tria.ge",
        "ads.malware-traffic-analysis.net", "analytics.malware-traffic-analysis.net"
    )

    // Ad URL patterns
    private val adPatterns = listOf(
        "/ads/", "/ad/", "/advert/", "/advertisement/",
        "/banner/", "/pop/", "/popup/", "/popunder/",
        "/track/", "/tracker/", "/tracking/", "/analytics/",
        "/metric/", "/metrics/", "/pixel/", "/beacon/", "/telemetry/",
        "/stat/", "/stats/", "/counter/", "/count/", "/log/", "/logs/",
        "/tag/", "/tags/", "/script/", "/scripts/",
        "googletag", "googleads", "googlesyndication", "google-analytics",
        "doubleclick", "facebook.com/tr", "connect.facebook.net",
        "adsystem", "adservice", "adserver", "adtech",
        "adnxs", "appnexus", "rubicon", "openx",
        "pubmatic", "criteo", "outbrain", "taboola",
        "revcontent", "mgid", "adform", "adroll",
        "adzerk", "adblade", "adsterra", "popads",
        "popcash", "propellerads", "adcash", "exoclick",
        "juicyads", "bidvertiser", "clicksor", "chitika",
        "infolinks", "kontera", "vibrantmedia", "intellitxt",
        "quantserve", "scorecardresearch", "comscore", "nielsen",
        "moatads", "iasds", "doubleverify", "integralads",
        "forensiq", "pixalate", "whiteops", "tamome",
        "adloox", "adsafeprotected", "geoedge", "confiant",
        "themediatrust", "adlightning", "clean.io", "adomik",
        "adthrive", "mediate", "adpushup", "ezoic",
        "monumetric", "shemedia", "freestar", "sortable",
        "headerlift", "pubgalaxy", "adrecover", "adblockanalytics",
        "adblockplus", "ublock", "easylist", "fanboy",
        "adguard", "ghostery", "privacybadger", "disconnect",
        "ublockorigin", "adnauseam", "blokada", "dns66",
        "adaway", "hosts-file", "someonewhocares", "winhelp2002",
        "pgl.yoyo", "malwaredomainlist", "phishing.army", "urlhaus",
        "threatfox", "bazaar", "malware.bazaar", "virustotal",
        "hybrid-analysis", "any.run", "joesandbox", "cuckoo",
        "malwr", "virscan", "metadefender", "opswat",
        "reversinglabs", "lastline", "vmray", "hatching",
        "tria", "malware-traffic-analysis"
    )

    fun isAd(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // Check domain
        for (domain in adDomains) {
            if (lowerUrl.contains(domain.lowercase())) {
                return true
            }
        }

        // Check patterns
        for (pattern in adPatterns) {
            if (lowerUrl.contains(pattern.lowercase())) {
                return true
            }
        }

        return false
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream("".toByteArray())
        )
    }
}
