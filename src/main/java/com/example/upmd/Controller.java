package com.example.upmd;


import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@org.springframework.stereotype.Controller
public class Controller {
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);
    String url = "";
    String token = "";
    String pcpath = "";
    @RequestMapping("/")
    public String mmain(){
        System.out.println(System.getProperty("user.dir"));
        return "Test";
    }

    @PostMapping("/upmd")
    public String upmd(@RequestParam("file") MultipartFile file,@RequestParam("halourl") String halourl,@RequestParam("pcpcpath") String pcpcpath) throws Exception{
        url = halourl;
        pcpath = pcpcpath;
        gettoken();
        String pcpathnew = pcpath;
        File mdfile = null;
        if (file.equals("") || file.getSize() <= 0) {
            file = null;
        } else {
            InputStream ins = null;
            ins = file.getInputStream();
            mdfile = new File(file.getOriginalFilename());
            inputStreamToFile(ins, mdfile);
            ins.close();
            pcpathnew = pcpathnew+mdfile.getName();
        }

        String encoding = "utf-8";
        BufferedReader bufReader = null;
        try{
            bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(mdfile)));
            //判断文件是否存在
            if (mdfile.isFile() && mdfile.exists()) {
                StringBuilder strBuilder = new StringBuilder();
                String lineTxt;
                while ((lineTxt = bufReader.readLine()) != null) {
                    if (lineTxt.indexOf("image-") != -1) { //判断当前行是否存在想要替换掉的字符 -1表示存在
                        String imagepath = lineTxt.substring(lineTxt.indexOf("(")+1, lineTxt.indexOf(")"));
                        AjaxResult ajaxResult = new AjaxResult();
                        ajaxResult = uploadImage(imagepath,"/api/admin/attachments/upload");
                        String yunpath = (String) ajaxResult.get(AjaxResult.DATA_TAG);
                        String imagename = (String) ajaxResult.get(AjaxResult.DATA_NAME);
                        lineTxt = "!["+imagename+"]("+yunpath+")";
                    }
                    strBuilder.append(lineTxt);
                    strBuilder.append(System.lineSeparator());//行与行之间的分割
                }

                File pcfile = new File(pcpathnew);
                try {
                    pcfile.createNewFile();
                    System.out.println("文件创建成功");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedWriter bufWriter = null;
                try{
                    bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pcfile)));
                    bufWriter.write(strBuilder.toString());
                    bufWriter.flush();
                }finally {
                    bufWriter.close();
                }

            } else {
                System.out.println("找不到指定的文件");
            }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
        }finally {
            bufReader.close();
        }

        uploadImage(pcpathnew,"/api/admin/backups/markdown/import");
        System.out.println("md上传成功");
        return "success";
    }


    private static void inputStreamToFile(InputStream ins, File file) {
        try {
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            logger.info("读文件错误{}",e);
        }
    }

    public void gettoken(){
        System.out.println("获取token");
        String tokenurl = url+"/api/admin/login";
        //获取请求头
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("Accept", "*/*");
        headerMap.put("connection", "Keep-Alive");
        headerMap.put("Content-Type", "application/json");

        //获取请求体
        JSONObject obj = new JSONObject();
        obj.put("authcode", "123456");
        obj.put("password", "123Lyr123");
        obj.put("username", "cyli");

        HttpClientUtil httpClientUtil = new HttpClientUtil();
        //发送post请求
        String results = httpClientUtil.doPost(tokenurl, headerMap, obj.toJSONString(), "UTF-8");
        //返回结果
        System.out.println(results);
        JSONObject resultJson = JSONObject.parseObject(results);
        JSONObject data = JSONObject.parseObject(resultJson.getString("data"));
        token =data.getString("access_token");
        return ;
    }


    /**
     * 上传文件接口
     *
     * @return 返回结果
     */
    public AjaxResult uploadImage(String path,String pcpcpath) {
        File image = new File(path);
        MultipartFile file = null;
        try {
            file = new MockMultipartFile(image.getName(), image.getName(), null, new FileInputStream(image));
        } catch (IOException e) {
            e.printStackTrace();
        }
        AjaxResult ajaxResult = upload(file,pcpcpath);
        String deletepath = System.getProperty("user.dir")+"/"+image.getName();
        System.out.println(deletepath);
        File deletefile = new File(deletepath);
        if (deletefile.delete()) {
            System.out.println("File deleted successfully");
        }
        else {
            System.out.println("Failed to delete the file");
        }
        return ajaxResult;
    }

    /**
     * 上传核心
     * @param file 文件流
     * @return 返回结果
     */
    public AjaxResult upload(MultipartFile file,String lastpath) {
        AjaxResult ajaxResult = new AjaxResult();

        try {
            String url = this.url + lastpath;
            String res = JPost(url,file,this.token);
            if ("".equals(res) || res == null) {
                logger.info("上传文件失败,调用官网接口返回结果为空{}", res);
                ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.ERROR);
                ajaxResult.put(AjaxResult.MSG_TAG, "上传文件失败,调用官网接口返回结果为空");
                return ajaxResult;
            }
            //将返回数据转换成json
            JSONObject jsonObject = JSONObject.parseObject(res);
            String message = (jsonObject.get("message")).toString();
            String code = (jsonObject.get("status")).toString();
            JSONObject dataObject = (JSONObject) jsonObject.get("data");
            String path = dataObject.getString("path");
            String name = dataObject.getString("name");
            //TODO 需要将返回的图片路径进行保存
            if ("200".equals(code)) {
                ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.SUCCESS);
                ajaxResult.put(AjaxResult.MSG_TAG, message);
                ajaxResult.put(AjaxResult.DATA_TAG, path);
                ajaxResult.put(AjaxResult.DATA_NAME, name);
            } else {
                ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.ERROR);
                ajaxResult.put(AjaxResult.MSG_TAG, message);
            }
            logger.info("上传文件接口调用结束结束,响应结果{}", res);


        } catch (Exception e) {
            ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.ERROR);
            ajaxResult.put(AjaxResult.MSG_TAG, "调用上传文件接口异常");
            logger.info("调用上传文件接口异常,错误信息{}", e.getMessage());
        }
        return ajaxResult;
    }

    /**
     *
     *  发送文件MultipartFile类型的参数请求第三方接口
     * @param url  请求url
     * @param file 参数
     * @return 字符流
     * @throws IOException
     */
    public String JPost(String url, MultipartFile file, String token) throws IOException {
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("file", new FileSystemResource(convert(file)));

        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "*/*");
        headers.add("connection", "Keep-Alive");
        headers.add("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        headers.add("Accept-Charset", "utf-8");
        headers.add("Content-Type", "application/json; charset=utf-8");
        headers.add("Admin-Authorization", token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        String body = response.getBody();
        return body;
    }

    /**
     * 接收处理传过来的文件
     * @param file MultipartFile 类型的文件
     * @return
     */
    public static File convert(MultipartFile file) {
        File convFile = new File(file.getOriginalFilename());
        try {
            convFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(convFile);
            fos.write(file.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return convFile;
    }

}
