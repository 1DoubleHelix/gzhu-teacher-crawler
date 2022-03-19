import com.csvreader.CsvWriter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Crawler {
    public static void main(String[] args) {
        CloseableHttpClient httpClient = HttpClients.createDefault();//创建HttpClient对象
        HttpGet httpGet = new HttpGet("http://jsj.gzhu.edu.cn/szdw1/jsjkxywlgcxysz.htm");//创建HttpGet对象，设置访问url地址
        Map<Integer, String> linkMap = new HashMap<>();//存放教师个人页面链接
        Map<Integer, String> nameMap = new HashMap<>();//存放姓名
        Map<Integer, String> infoMap = new HashMap<>();//存放个人信息

        //连接池管理器
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        //设置连接数
        cm.setMaxTotal(100);
        //每个主机最大连接数
        cm.setDefaultMaxPerRoute(10);

        CloseableHttpResponse response = null;
        try {
            //使用HttpClient发起请求，获取response
            response = httpClient.execute(httpGet);
//            System.out.println(response);

            //解析响应
            if (response.getStatusLine().getStatusCode() == 200) {
                String mainContent = EntityUtils.toString(response.getEntity(), "utf8");
                Document document = Jsoup.parse(mainContent);

                //抓取所有教师页面的子链接
                Elements mainElem = document.select(".clearfloat .mclb a");

                int linkNum = 0;
                for (Element element : mainElem) {
                    String href = element.attr("href");
                    linkMap.put(linkNum, href.substring(2));//存储链接
                    linkNum++;
                }

                for (int i = 0; i < linkNum; i++) {
                    //使用连接池管理器发起请求
                    String teacherContent = doGet(cm, linkMap.get(i));
                    //获取教师页面
                    Document teacherDoc = Jsoup.parse(teacherContent);
                    Elements elemName = teacherDoc.select("[name=_newscontent_fromname] h2");
                    nameMap.put(i, elemName.text());

                    //获取教师信息
                    Elements elemInfo = teacherDoc.select("[id=vsb_content] p span");
                    infoMap.put(i, elemInfo.text());

                    write(nameMap, infoMap);
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭response
                assert response != null;
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                //关闭httpClient
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String doGet(PoolingHttpClientConnectionManager cm, String link) {
        //从连接池获取HttpClient对象
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        HttpGet httpGet = new HttpGet("http://jsj.gzhu.edu.cn" + link);//访问教师个人主页
//        System.out.println("http://jsj.gzhu.edu.cn" + link);
        String content = null;

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            {
                if (response.getStatusLine().getStatusCode() == 200) {
                    //返回教师页面
                    content = EntityUtils.toString(response.getEntity(), "utf8");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content;
    }

    public static void write(Map name, Map info) {
        String filePath = "teacher.csv";
        //创建csv写对象
        CsvWriter csvWriter = new CsvWriter(filePath, ',', Charset.forName("GBK"));
        String[] headers = {"姓名", "职称", "系、研究所", "研究领域"};
        try {
            //写表头
            csvWriter.writeRecord(headers);
            //写数据
            for (int i = 0; i < name.size(); i++) {
                String[] content = {name.get(i).toString(), Arrays.toString(info.get(i).toString().split("\\s"))};
                csvWriter.writeRecord(content);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        csvWriter.close();
    }

}
