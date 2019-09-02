package com.demo.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.entities.Images;
import com.demo.entities.User;
import com.demo.service.ImagesService;
import com.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author laojie
 * @since 2019-08-29
 */
@Controller
@RequestMapping("/images")
public class ImagesController {

    @Autowired
    private ImagesService imagesService;


    @RequestMapping("/toUpload")
    public String toUpload(){
        return "upload";
    }

    @RequestMapping("/imageList")
    public String imageList(@RequestParam(value = "page", required = false, defaultValue = "1") int page
            ,@RequestParam(value = "selectMsg",required = false) String selectMsg,HttpServletRequest request, Model model) {
        int count = imagesService.count();
        int totalPage=(count+2)/3;
        if(page>totalPage)page=totalPage;
        Page<Images> imagesPage = new Page<>(page, 3);
        QueryWrapper<Images> imagesQueryWrapper = new QueryWrapper<>();
        if(null!=selectMsg&&!selectMsg.trim().equals("")){
            imagesQueryWrapper.like("remark",selectMsg).or().like("img_url",selectMsg);
            model.addAttribute("selectMsg",selectMsg);
        }
        imagesQueryWrapper.orderByDesc("create_time");
        IPage<Images> iPage = imagesService.page(imagesPage,imagesQueryWrapper);
        model.addAttribute("images", iPage.getRecords());
        model.addAttribute("nowPage", iPage.getCurrent());
        model.addAttribute("total", iPage.getTotal());
        model.addAttribute("pages",iPage.getPages());
        return "fileList";
    }

    /**
     * 图片上传
     *
     * @param files    图片文件数组
     * @param remark  该图片的备注  可以为null
     * @param request
     * @return
     */
    @RequestMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile[] files, @RequestParam(value = "remark",required = false,defaultValue = "-")
            String remark,@RequestParam("filename") String filename, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);
        List<String> result=new ArrayList<>();//保存文件的上传结果
        for(int i=0;i<files.length;i++){

            if(!files[i].isEmpty()){
                //获取后缀
                String suffix = files[i].getOriginalFilename().substring(files[i].getOriginalFilename().lastIndexOf("."));
                //判断后缀
                if (!suffix.equalsIgnoreCase(".jpg")
                        && !suffix.equalsIgnoreCase(".png")
                        && !suffix.equalsIgnoreCase(".jpeg")
                        && !suffix.equalsIgnoreCase(".gif")) {
                    result.add("第"+(i+1)+"个文件，保存失败，请选择图片文件。");
                    break;
                }
                //为保存的图片取名字
                String fileName=null;
                if(null!=filename&&!filename.trim().equals(""))//如果自定义名不为空
                    fileName=filename+(i+1);
                else fileName = UUID.randomUUID().toString();
                //存储图片
                //获取存储路径
                File rootPath = new File(request.getServletContext().getRealPath(""));
                String strRootPath = "C:\\app\\apache-tomcat-8.5.43\\webapps";
                String savePath=strRootPath + "\\ROOT\\images\\" + fileName + suffix;
                Images images = new Images();
                images.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                images.setRemark(remark);
                images.setImgUrl("http://localhost:8080/images/" + fileName + suffix);
//                if(files.length==1)
                if(saveOrUpdateFile(files[i],savePath,images,0))
                    result.add("第"+(i+1)+"个文件保存成功。");
                else result.add("第"+(i+1)+"个文件保存失败。");
            }
        }
        session.setAttribute("msgList",result);
        return "redirect:/images/imageList";
    }
    @RequestMapping("/delete")
    public String delete(@RequestParam("id") int id,HttpServletRequest request){
        Images image = imagesService.getById(id);
        //要删除的文件名
        String imgPath=image.getImgUrl().substring(image.getImgUrl().lastIndexOf("/")+1);
        //获取存储路径
        File rootPath = new File(request.getServletContext().getRealPath(""));
        String strRootPath = "C:\\app\\apache-tomcat-8.5.43\\webapps\\ROOT\\images";//根目录
        deleteFile(strRootPath,imgPath);
        imagesService.removeById(id);
        request.getSession().setMaxInactiveInterval(5);
        request.getSession().setAttribute("msg","删除成功。");
        return "redirect:/images/imageList";
    }

    @RequestMapping("/update")
    public String update(@RequestParam(value = "file",required = false) MultipartFile file,
                         @RequestParam(value = "remark",required = false) String remark,@RequestParam("id") int id,HttpServletRequest request){
        HttpSession session = request.getSession();
        Images image = imagesService.getById(id);Images i=new Images();
        i.setId(image.getId());
        i.setImgUrl(image.getImgUrl());
        i.setRemark(remark);
        i.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        if(null!=file){
            //要删除的文件名
            String imgPath=image.getImgUrl().substring(image.getImgUrl().lastIndexOf("/")+1);
            //获取存储路径
//            File rootPath = new File(request.getServletContext().getRealPath(""));//
            String strRootPath = "C:\\app\\apache-tomcat-8.5.43\\webapps\\ROOT\\images";//根目录
            deleteFile(strRootPath,imgPath);
            if(saveOrUpdateFile(file,strRootPath+"\\"+imgPath,i,1))
                session.setAttribute("msg","更新成功。");
            else session.setAttribute("msg","更新失败。");
        }
        if(null==file&&(null!=remark||!remark.trim().equals(""))){
            if(imagesService.updateById(image))session.setAttribute("msg","更新成功。");
            else session.setAttribute("msg","更新失败。");
        }
        return "redirect:/images/imageList";
    }

    /**
     * 更新或保存文件
     * @param file 文件
     * @param savePath 保存的路径
     * @param images 要保存的images对象
     * @param saveType 保存的类型，0 保存，1 更新
     * @return
     */
    public boolean saveOrUpdateFile(MultipartFile file, String savePath, Images images, int saveType){
        //获取后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        //判断后缀
        if (!suffix.equalsIgnoreCase(".jpg")
                && !suffix.equalsIgnoreCase(".png")
                && !suffix.equalsIgnoreCase(".jpeg")
                && !suffix.equalsIgnoreCase(".gif")) {
           return false;
        }
        File target = new File(savePath);
        try {
            if(saveType==0)//0保持，1更新
                imagesService.save(images);
            else
                imagesService.updateById(images);
            file.transferTo(target);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteFile(String rootPath,String deleteFileName){
        File[] files = new File(rootPath).listFiles();//获取根目录下所有文件
        for(File file:files){
            if(file.toString().substring(file.toString().lastIndexOf("\\")+1).equals(deleteFileName))
                file.delete();
        }
    }

}
