package com.lee.dev;

import com.csvreader.CsvWriter;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.xiaoleilu.hutool.http.HttpUtil;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MyCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern
            .compile(".*(\\.(css|js|bmp|gif|jpe?g|ico"
                    + "|png|tiff?|mid|mp2|mp3|mp4"
                    + "|wav|avi|mov|mpeg|ram|m4v|pdf"
                    + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

    private final static String URL_PREFIX = "http://www.souche.com/pages/onsale/sale_car_list.html?";
    private final static Pattern URL_PARAMS_PATTERN = Pattern
            .compile("carbrand=brand-\\d+(&index=\\d+)?");

    private final static String CSV_PATH = "d:/data.csv";
    private CsvWriter cw;
    private File csv;
    private String analysisType = "jieba";

    public MyCrawler() throws IOException {
        csv = new File(CSV_PATH);

        if (csv.isFile()) {
            csv.delete();
        }

        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
        cw = new CsvWriter(writer, ',');
        cw.write("一级目录");
        cw.write("二级目录");
        cw.write("URL");
        cw.write("标题");
        cw.write("分词");
        cw.endRecord();
        cw.close();
    }

    /**
     * You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic).
     */
    @Override
    public boolean shouldVisit(Page page, WebURL url) {
        String href = url.getURL().toLowerCase();
        if (FILTERS.matcher(href).matches() || !href.startsWith(URL_PREFIX)) {
            return false;
        }

        String[] strs = href.split("\\?");
        if (strs.length < 2) {
            return false;
        }

        if (!URL_PARAMS_PATTERN.matcher(strs[1]).matches()) {
            return false;
        }

        return true;
    }

    /**
     * This function is called when a page is fetched and ready to be processed
     * by your program.
     */
    @Override
    public void visit(Page page) {
        ToAnalysis.parse("");
        String pageUrl = page.getWebURL().getURL();

        if (page.getParseData() instanceof HtmlParseData) {  // 判断是否是html数据
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData(); // 强制类型转换，获取html数据对象
            Document doc = Jsoup.parse(htmlParseData.getHtml());
            Integer pageType = 0;
            //时事新闻
            if (pageUrl.indexOf("guonei") > 0) {
                pageType = 1;
                parseNews(doc, pageType);
            }
            if (pageUrl.indexOf("guoji") > 0) {
                pageType = 2;
                parseNews(doc, pageType);
            }
            if (pageUrl.indexOf("society") > 0) {
                pageType = 3;
                parseNews(doc, pageType);
            }
            //军事
            if (pageUrl.indexOf("mil") > 0) {
                getMilData();
            }
            //财经
            if (pageUrl.indexOf("finance") > 0) {
                getFinanceData();
            }
            //娱乐
            if (pageUrl.indexOf("ent") > 0) {
                getEntData();
            }
            //体育
            if (pageUrl.indexOf("sports") > 0) {
                getSportData(doc);
            }
            //科技
            if (pageUrl.indexOf("tech") > 0) {
                getInternetData();
                getDigitalData(doc);
                getPhoneData(doc);
            }
            //游戏
            if (pageUrl.indexOf("game") > 0) {
                getEsportData();
                getNetGameData();
            }
            //时尚
            if (pageUrl.indexOf("lady") > 0) {
                getLadyData();
            }
            //汽车
            if (pageUrl.indexOf("auto") > 0) {
                getAutoData();
            }
            //房产
            if (pageUrl.indexOf("house") > 0) {
                getHouseData(doc);
            }

        }
    }

    /**
     * 处理 国内新闻，国际新闻，社会新闻
     */
    private void parseNews(Document doc, Integer pageType) {
        Elements news = doc.select("ul.ulist");
        for (Element c : news) {
            if (!c.hasClass("mix-ulist")) {
                Elements newInfo = c.children();
                for (Element info : newInfo) {
                    String newUrl = info.child(0).attr("href");
                    String newTitle = info.child(0).text();
                    String resultStr = analysisTilte(newTitle, analysisType);
                    try {
                        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                        cw = new CsvWriter(writer, ',');
                        cw.write("时事新闻");
                        switch (pageType) {
                            case 1:
                                cw.write("国内");
                                break;
                            case 2:
                                cw.write("国际");
                                break;
                            case 3:
                                cw.write("社会");
                                break;
                            default:
                                cw.write("");
                                break;
                        }
                        cw.write(newUrl);
                        cw.write(newTitle);
                        cw.write(resultStr);
                        cw.endRecord();
                        cw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    /**
     * 处理 军事
     */
    private void getMilData() {
        String chinaResult = HttpUtil.get("http://news.baidu.com/widget?id=ChinaMil&channel=mil&t=" + Long.toString(new Date().getTime()));
        String internationalResult = HttpUtil.get("http://news.baidu.com/widget?id=InternationalMil&channel=mil&t=" + Long.toString(new Date().getTime()));
        parseMilData(chinaResult, 1);
        parseMilData(internationalResult, 2);
    }

    /**
     * 转换军事请求的数据
     *
     * @param dateString
     */
    private void parseMilData(String dateString, Integer channelType) {
        Document doc = Jsoup.parse(dateString);
        Element title = doc.select("h2 a").first();
        String pageTitle = title.text();
        Elements news = doc.select("ul.ulist");
        for (Element c : news) {
            Elements newInfo = c.children();
            for (Element info : newInfo) {
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("军事");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理 财经
     */
    private void getFinanceData() {
        String stockResult = HttpUtil.get("http://news.baidu.com/widget?id=Stock&channel=finance&t=" + Long.toString(new Date().getTime()));
        String financeResult = HttpUtil.get("http://news.baidu.com/widget?id=Finances&channel=finance&t=" + Long.toString(new Date().getTime()));
        String economyResult = HttpUtil.get("http://news.baidu.com/widget?id=Economy&channel=finance&t=" + Long.toString(new Date().getTime()));
        parseFinanceData(stockResult, 1);
        parseFinanceData(financeResult, 2);
        parseFinanceData(economyResult, 3);
    }

    /**
     * 转换财经请求的数据
     *
     * @param dateString
     */
    private void parseFinanceData(String dateString, Integer channelType) {
        Document doc = Jsoup.parse(dateString);
        Element title = doc.select("h2 a").first();
        String pageTitle = title.text();
        if (channelType == 1 || channelType == 2) {
            Elements news = doc.select("ul.ulist");
            Element newInfo = news.get(0);
            for (Element c : newInfo.children()) {
                String newUrl = c.child(0).attr("href");
                String newTitle = c.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("财经");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            newInfo = news.get(2);
            for (Element c : newInfo.children()) {
                String newUrl = c.child(0).attr("href");
                String newTitle = c.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("财经");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (channelType == 3) {
            Elements news = doc.select("ul.ulist");
            for (int index = 2; index <= 3; index++) {
                Element newInfo = news.get(index);
                for (Element c : newInfo.children()) {
                    String newUrl = c.child(0).attr("href");
                    String newTitle = c.child(0).text();
                    String resultStr = analysisTilte(newTitle, analysisType);
                    try {
                        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                        cw = new CsvWriter(writer, ',');
                        cw.write("财经");
                        cw.write(pageTitle);
                        cw.write(newUrl);
                        cw.write(newTitle);
                        cw.write(resultStr);
                        cw.endRecord();
                        cw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 处理 娱乐
     */
    private void getEntData() {
        String starResult = HttpUtil.get("http://news.baidu.com/widget?id=Star&channel=ent&t=" + Long.toString(new Date().getTime()));
        String movielResult = HttpUtil.get("http://news.baidu.com/widget?id=Movie&channel=ent&t=" + Long.toString(new Date().getTime()));
        String tvResult = HttpUtil.get("http://news.baidu.com/widget?id=TV&channel=ent&t=" + Long.toString(new Date().getTime()));
        String musicResult = HttpUtil.get("http://news.baidu.com/widget?id=Music&channel=ent&t=" + Long.toString(new Date().getTime()));
        String varietyResult = HttpUtil.get("http://news.baidu.com/widget?id=Variety&channel=ent&t=" + Long.toString(new Date().getTime()));
        parseEntData(starResult, 1);
        parseEntData(movielResult, 2);
        parseEntData(tvResult, 3);
        parseEntData(musicResult, 4);
        parseEntData(varietyResult, 5);

    }

    /**
     * 转换娱乐请求的数据
     *
     * @param dateString
     */
    private void parseEntData(String dateString, Integer channelType) {
        Document doc = Jsoup.parse(dateString);
        Element title = doc.select("h2 a").first();
        String pageTitle = title.text();
        Elements news = doc.select("ul.ulist");

        for (int index = 0; index <= 1; index++) {
            Element c = news.get(index);
            Elements newInfo = c.children();
            for (Element info : newInfo) {
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("娱乐");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理 体育
     */
    private void getSportData(Document doc) {
        String chinaSoccerResult = HttpUtil.get("http://news.baidu.com/widget?id=ChinaSoccerNews&channel=sports&t=" + Long.toString(new Date().getTime()));
        String worldSoccerResult = HttpUtil.get("http://news.baidu.com/widget?id=WorldSoccerNews&channel=sports&t=" + Long.toString(new Date().getTime()));
        String cbaNewsResult = HttpUtil.get("http://news.baidu.com/widget?id=CbaNews&channel=sports&t=" + Long.toString(new Date().getTime()));
        String otherNewsResult = HttpUtil.get("http://news.baidu.com/widget?id=OtherNews&channel=sports&t=" + Long.toString(new Date().getTime()));
        parseSportData(chinaSoccerResult, 1);
        parseSportData(worldSoccerResult, 2);
        //NBA 需要单独处理
        parseNbaData(doc);
        parseSportData(cbaNewsResult, 4);
        parseSportData(otherNewsResult, 5);

    }

    /**
     * 转换体育请求的数据
     *
     * @param dateString
     */
    private void parseSportData(String dateString, Integer channelType) {
        Document doc = Jsoup.parse(dateString);
        Element title = doc.select("h2 a").first();
        String pageTitle = title.text();
        Elements news = doc.select("ul.ulist");

        for (int index = 0; index <= 1; index++) {
            Element c = news.get(index);
            Elements newInfo = c.children();
            for (Element info : newInfo) {
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("体育");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 转换NBA请求的数据
     *
     * @param doc
     */
    private void parseNbaData(Document doc) {
        Element title = doc.select("h2 a").first();
        String pageTitle = title.text();
        Elements news = doc.select("ul.ulist");

        for (int index = 2; index <= 3; index++) {
            Element c = news.get(index);
            Elements newInfo = c.children();
            for (Element info : newInfo) {
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("体育");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解析互联网的新闻
     */
    private void getInternetData() {
        String techResult = HttpUtil.get("http://tech.baidu.com/n?cmd=1&class=internet&pn=1&from=tab", "GBK");
        Document doc = Jsoup.parse(techResult);
        Elements elements = doc.select("h3");
        for (int eleIndex = 0; eleIndex < 13; eleIndex++) {
            Element info = elements.get(eleIndex);
            String newUrl = info.child(0).attr("href");
            String newTitle = info.child(0).text();
            String resultStr = analysisTilte(newTitle, analysisType);
            try {
                Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                cw = new CsvWriter(writer, ',');
                cw.write("科技");
                cw.write("互联网");
                cw.write(newUrl);
                cw.write(newTitle);
                cw.write(resultStr);
                cw.endRecord();
                cw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析数码的新闻
     */
    private void getDigitalData(Document doc) {
        Elements elements = doc.select(".fb-list");
        for (int eleIndex = 7; eleIndex < 9; eleIndex++) {
            Element news = elements.get(eleIndex);
            Elements infos = news.children();
            for (Element info : infos) {
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("科技");
                    cw.write("数码");
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解析手机的新闻
     */
    private void getPhoneData(Document doc) {
        Elements elements = doc.select(".ulist");
        for (int eleIndex = 7; eleIndex <= 9; eleIndex++) {
            Element news = elements.get(eleIndex);
            Elements infos = news.children();
            Integer forSize = 3;
            if (eleIndex == 9) {
                forSize = 6;
            }
            for (int index = 0; index < forSize; index++) {
                Element info = infos.get(index);
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("科技");
                    cw.write("手机");
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解析电子竞技的新闻
     */
    private void getEsportData() {
        String esportResult = HttpUtil.get("http://news.baidu.com/n?cmd=1&class=dianzijingji&pn=1", "GBK");
        Document doc = Jsoup.parse(esportResult);
        Elements elements = doc.select(".list");
        int count = 1;
        for (int eleIndex = 0; eleIndex <= 2; eleIndex++) {
            Element news = elements.get(eleIndex);
            Elements infos = news.children();
            for (Element info : infos) {
                if (count > 12) {
                    break;
                }
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("游戏");
                    cw.write("电子竞技");
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                count++;
            }
        }
    }

    /**
     * 解析网游新闻
     *
     * @param
     */
    private void getNetGameData() {
        String netGanemResult = HttpUtil.get("http://news.baidu.com/n?cmd=1&class=netgames&pn=1", "GBK");
        Document doc = Jsoup.parse(netGanemResult);
        Elements elements = doc.select(".list");
        int count = 1;
        for (int eleIndex = 0; eleIndex <= 2; eleIndex++) {
            Element news = elements.get(eleIndex);
            Elements infos = news.children();
            for (Element info : infos) {
                if (count > 12) {
                    break;
                }
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("游戏");
                    cw.write("网络游戏");
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                count++;
            }
        }
    }

    /**
     * 处理 时尚
     */
    private void getLadyData() {
        String trendFashionResult = HttpUtil.get("http://news.baidu.com/widget?id=TrendFashion&channel=lady&t=" + Long.toString(new Date().getTime()));
        String skinCareResult = HttpUtil.get("http://news.baidu.com/widget?id=SkinCare&channel=lady&t=" + Long.toString(new Date().getTime()));
        parseLadyData(trendFashionResult, 1);
        parseLadyData(skinCareResult, 2);
    }

    /**
     * 转换时尚请求的数据
     *
     * @param dateString
     */
    private void parseLadyData(String dateString, Integer channelType) {
        Document doc = Jsoup.parse(dateString);
        Element title = doc.select("h2").first();
        String pageTitle = title.text();
        if (pageTitle.length() > 4) {
            pageTitle = pageTitle.substring(0, 4);
        }
        Elements news = doc.select("ul.ulist");
        for (int eleIndex = 0; eleIndex < 2; eleIndex++) {
            Element c = news.get(eleIndex);
            Elements newInfo = c.children();
            for (Element info : newInfo) {
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("时尚");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理 娱乐
     */
    private void getAutoData() {
        String autoResult = HttpUtil.get("http://news.baidu.com/widget?id=Related&channel=auto&t=" + Long.toString(new Date().getTime()));
        Document doc = Jsoup.parse(autoResult);
        Element title = doc.select("h2 a").first();
        String pageTitle = title.text();
        Elements news = doc.select("ul.ulist");
        int count = 1;
        for (int eleIndex = 0; eleIndex < 2; eleIndex++) {
            Element c = news.get(eleIndex);
            Elements newInfo = c.children();
            for (Element info : newInfo) {
                if (count > 10) {
                    break;
                }
                String newUrl = info.child(0).attr("href");
                String newTitle = info.child(0).text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("汽车");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                count++;
            }
        }
    }

    private void getHouseData(Document doc) {
        Element title = doc.select(".arrow").first();
        String pageTitle = title.text();
        Elements newDivs = doc.select(".tlc");
        for (int newdivIndex = 0; newdivIndex < 2; newdivIndex++) {
            Element newDiv = newDivs.get(newdivIndex);
            Elements newInfos = newDiv.select("a");
            for (Element info : newInfos) {
                String newUrl = info.attr("href");
                String newTitle = info.text();
                String resultStr = analysisTilte(newTitle, analysisType);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_PATH, true), "GBK"));
                    cw = new CsvWriter(writer, ',');
                    cw.write("房产");
                    cw.write(pageTitle);
                    cw.write(newUrl);
                    cw.write(newTitle);
                    cw.write(resultStr);
                    cw.endRecord();
                    cw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String analysisTilte(String title, String type) {
        String resultStr = "";
        if (type.equals("ansj")) {
            Result result = ToAnalysis.parse(title); //分词结果的一个封装，主要是一个List<Term>的terms
            List<Term> terms = result.getTerms(); //拿到terms
            for (int i = 0; i < terms.size(); i++) {
                resultStr = resultStr.concat(terms.get(i).getName()).concat(","); //拿到词
            }
            if (resultStr.length() > 1) {
                resultStr = resultStr.substring(0, resultStr.length() - 1);
            }
        }
        if (type.equals("jieba")) {
            JiebaSegmenter segmenter = new JiebaSegmenter();
            resultStr = segmenter.sentenceProcess(title).toString();
        }
        return resultStr;
    }

}