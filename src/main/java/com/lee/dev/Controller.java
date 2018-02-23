package com.lee.dev;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Controller {
    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "d:/crawData/root";
        int numberOfCrawlers = 7;
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setUserAgentName("Baiduspider");
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        //时事新闻
        controller.addSeed("http://news.baidu.com/guonei");
        controller.addSeed("http://news.baidu.com/guoji");
        controller.addSeed("http://news.baidu.com/society");

        //军事
        controller.addSeed("http://news.baidu.com/mil");

        //财经
        controller.addSeed("http://news.baidu.com/finance");

        //娱乐
        controller.addSeed("http://news.baidu.com/ent");

        //体育
        controller.addSeed("http://news.baidu.com/sports");

        //科技
        controller.addSeed("http://news.baidu.com/tech");

        //游戏
        controller.addSeed("http://news.baidu.com/game");

        //时尚
        controller.addSeed("http://news.baidu.com/lady");

        //汽车
        controller.addSeed("http://news.baidu.com/auto");

        //房产
        controller.addSeed("http://news.baidu.com/house");
        controller.start(MyCrawler.class, numberOfCrawlers);

    }
}