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
            //????????????????????????
            if (mdfile.isFile() && mdfile.exists()) {
                StringBuilder strBuilder = new StringBuilder();
                String lineTxt;
                while ((lineTxt = bufReader.readLine()) != null) {
                    if (lineTxt.indexOf("image-") != -1) { //??????????????????????????????????????????????????? -1????????????
                        String imagepath = lineTxt.substring(lineTxt.indexOf("(")+1, lineTxt.indexOf(")"));
                        AjaxResult ajaxResult = new AjaxResult();
                        ajaxResult = uploadImage(imagepath,"/api/admin/attachments/upload");
                        String yunpath = (String) ajaxResult.get(AjaxResult.DATA_TAG);
                        String imagename = (String) ajaxResult.get(AjaxResult.DATA_NAME);
                        lineTxt = "!["+imagename+"]("+yunpath+")";
                    }
                    strBuilder.append(lineTxt);
                    strBuilder.append(System.lineSeparator());//????????????????????????
                }

                File pcfile = new File(pcpathnew);
                try {
                    pcfile.createNewFile();
                    System.out.println("??????????????????");
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
                System.out.println("????????????????????????");
            }
        } catch (Exception e) {
            System.out.println("????????????????????????");
        }finally {
            bufReader.close();
        }

        uploadImage(pcpathnew,"/api/admin/backups/markdown/import");
        System.out.println("md????????????");
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
            logger.info("???????????????{}",e);
        }
    }

    public void gettoken(){
        System.out.println("??????token");
        String tokenurl = url+"/api/admin/login";
        //???????????????
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("Accept", "*/*");
        headerMap.put("connection", "Keep-Alive");
        headerMap.put("Content-Type", "application/json");

        //???????????????
        JSONObject obj = new JSONObject();
        obj.put("authcode", "123456");
        obj.put("password", "123Lyr123");
        obj.put("username", "cyli");

        HttpClientUtil httpClientUtil = new HttpClientUtil();
        //??????post??????
        String results = httpClientUtil.doPost(tokenurl, headerMap, obj.toJSONString(), "UTF-8");
        //????????????
        System.out.println(results);
        JSONObject resultJson = JSONObject.parseObject(results);
        JSONObject data = JSONObject.parseObject(resultJson.getString("data"));
        token =data.getString("access_token");
        return ;
    }


    /**
     * ??????????????????
     *
     * @return ????????????
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
     * ????????????
     * @param file ?????????
     * @return ????????????
     */
    public AjaxResult upload(MultipartFile file,String lastpath) {
        AjaxResult ajaxResult = new AjaxResult();

        try {
            String url = this.url + lastpath;
            String res = JPost(url,file,this.token);
            if ("".equals(res) || res == null) {
                logger.info("??????????????????,????????????????????????????????????{}", res);
                ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.ERROR);
                ajaxResult.put(AjaxResult.MSG_TAG, "??????????????????,????????????????????????????????????");
                return ajaxResult;
            }
            //????????????????????????json
            JSONObject jsonObject = JSONObject.parseObject(res);
            String message = (jsonObject.get("message")).toString();
            String code = (jsonObject.get("status")).toString();
            JSONObject dataObject = (JSONObject) jsonObject.get("data");
            String path = dataObject.getString("path");
            String name = dataObject.getString("name");
            //TODO ??????????????????????????????????????????
            if ("200".equals(code)) {
                ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.SUCCESS);
                ajaxResult.put(AjaxResult.MSG_TAG, message);
                ajaxResult.put(AjaxResult.DATA_TAG, path);
                ajaxResult.put(AjaxResult.DATA_NAME, name);
            } else {
                ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.ERROR);
                ajaxResult.put(AjaxResult.MSG_TAG, message);
            }
            logger.info("????????????????????????????????????,????????????{}", res);


        } catch (Exception e) {
            ajaxResult.put(AjaxResult.CODE_TAG, HttpStatus.ERROR);
            ajaxResult.put(AjaxResult.MSG_TAG, "??????????????????????????????");
            logger.info("??????????????????????????????,????????????{}", e.getMessage());
        }
        return ajaxResult;
    }

    /**
     *
     *  ????????????MultipartFile????????????????????????????????????
     * @param url  ??????url
     * @param file ??????
     * @return ?????????
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
     * ??????????????????????????????
     * @param file MultipartFile ???????????????
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
