package com.demo.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.entities.Images;
import com.demo.service.ImagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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

    @Value("${image.server_url}")
    private String URL;
//    @Value("${image.root_path}")
//    private String rootPath;

    /**
     * 前往更新图片信息
     *
     * @param id    要跟新的id
     * @param model
     * @return
     */
    @RequestMapping("/toUpdate")
    public String toUpdate(@RequestParam("id") int id, Model model) {
        model.addAttribute("id", id);
        return "update";
    }

    /**
     * 移除session中的msgList
     * @return
     */
    @RequestMapping("/removeMsgList")
    public String removeMsgList(HttpServletRequest request){
        if(null!=request.getSession(false).getAttribute("msgList"))
        request.getSession(false).removeAttribute("msgList");
        return "200";
    }

    /**
     * 前往添加图片信息
     *
     * @return
     */
    @RequestMapping("/toUpload")
    public String toUpload() {
        return "upload";
    }

    @RequestMapping("/imageList")
    @Transactional
    public String imageList(@RequestParam(value = "page", required = false, defaultValue = "1") int page
            , @RequestParam(value = "selectMsg", required = false) String selectMsg, HttpServletRequest request, Model model) {
        QueryWrapper<Images> imagesQueryWrapper = new QueryWrapper<>();
        int count = 0;
        if (null != selectMsg && !selectMsg.trim().equals("")) {
            imagesQueryWrapper.like("remark", selectMsg).or().like("img_url", selectMsg);
            count = imagesService.count(imagesQueryWrapper);
            model.addAttribute("selectMsg", selectMsg);
        } else count = imagesService.count();
        int totalPage = (count + 9) / 10;
        if (page > totalPage) page = totalPage;
        Page<Images> imagesPage = new Page<>(page, 10);
        imagesQueryWrapper.orderByDesc("create_time");
        IPage<Images> iPage = imagesService.page(imagesPage, imagesQueryWrapper);
        model.addAttribute("images", iPage.getRecords());
        model.addAttribute("nowPage", iPage.getCurrent());
        model.addAttribute("total", iPage.getTotal());
        model.addAttribute("pages", iPage.getPages());
        return "fileList";
    }

    /**
     * 图片上传
     *
     * @param files   图片文件数组
     * @param remark  该图片的备注  可以为null
     * @param request
     * @return
     */
    @RequestMapping("/upload")
    @Transactional
    public String upload(@RequestParam("file") MultipartFile[] files, @RequestParam(value = "remark", required = false, defaultValue = "-")
            String remark, @RequestParam("filename") String filename, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> result = new ArrayList<>();//保存文件的上传结果
        for (int i = 0; i < files.length; i++) {

            if (!files[i].isEmpty()) {
                //获取后缀
                String suffix = files[i].getOriginalFilename().substring(files[i].getOriginalFilename().lastIndexOf("."));
                //判断后缀
                if (!suffix.equalsIgnoreCase(".jpg")
                        && !suffix.equalsIgnoreCase(".png")
                        && !suffix.equalsIgnoreCase(".jpeg")
                        && !suffix.equalsIgnoreCase(".gif")) {
                    result.add("第" + (i + 1) + "个文件，保存失败，请选择图片文件。");
                    break;
                }
                //为保存的图片取名字
                String fileName = null;
                if (null != filename && !filename.trim().equals(""))//如果自定义名不为空
                    fileName = filename + (i + 1);
                else fileName = UUID.randomUUID().toString();
                //存储图片
                //获取存储路径
                File rootPath = new File(request.getServletContext().getRealPath(""));
//                String strRootPath = "C:/app/apache-tomcat-8.5.43/webapps";
                String savePath = rootPath.getParent() + "/ROOT/images/" + fileName + suffix;
                Images images = new Images();
                images.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                images.setRemark(remark);
                images.setImgUrl(this.URL +"/images/" + fileName + suffix);
//                if(files.length==1)
                if (saveOrUpdateFile(files[i], savePath, images, 0))
                    result.add("第" + (i + 1) + "个文件保存成功。");
                else result.add("第" + (i + 1) + "个文件保存失败。");
            }
        }
        session.setAttribute("msgList", result);
        return "redirect:/images/imageList";
    }

    @RequestMapping("/delete")
    @ResponseBody
    @Transactional
    public String delete(@RequestParam("id") int id, HttpServletRequest request) {
        try {
            Images image = imagesService.getById(id);
            //要删除的文件名
            String imgPath = image.getImgUrl().substring(image.getImgUrl().lastIndexOf("/") + 1);
            //获取存储路径
            File rootPath = new File(request.getServletContext().getRealPath(""));
//            String strRootPath = "C:/app/apache-tomcat-8.5.43/webapps/ROOT/images";//根目录
            String strRootPath = rootPath.getParent()+"/ROOT/images";//根目录
            deleteFile(strRootPath, imgPath);
            imagesService.removeById(id);
        } catch (Exception e) {
            return "500";
        }
        return "200";
    }

    /**
     * 更新图片信息
     *
     * @param file    上传的文件
     * @param remark  图片备注
     * @param id      要更新的图片信息的id
     * @param request
     * @return
     */
    @RequestMapping("/update")
    @Transactional
    public String update(@RequestParam(value = "file", required = false) MultipartFile file,
                         @RequestParam(value = "remark", required = false) String remark,
                         @RequestParam("filename") String filename, @RequestParam("id") int id, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Images image = imagesService.getById(id);
        List<String> result = new ArrayList<>();
        //要删除或者操作的文件名
        String imgPath = image.getImgUrl().substring(image.getImgUrl().lastIndexOf("/") + 1);
        //获取存储路径
        String strRootPath = new File(request.getServletContext().getRealPath("")).getParent()+"/ROOT/images";//根目录
        //如果要更新备注
        if (null != remark && !remark.trim().equals("")) image.setRemark(remark);
        try {
            if (!file.isEmpty()) {
                if (deleteFile(strRootPath, imgPath)) {//如果删除成功
                    //获取后缀
                    String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
                    //判断后缀
                    if (!suffix.equalsIgnoreCase(".jpg")
                            && !suffix.equalsIgnoreCase(".png")
                            && !suffix.equalsIgnoreCase(".jpeg")
                            && !suffix.equalsIgnoreCase(".gif")) {
                        result.add("保存失败，请选择图片文件。");
                        return "redirect:/images/imageList";
                    }
                    //为保存的图片取名字
                    String fileName = null;
                    if (null != filename && !filename.trim().equals("")) {//如果自定义名不为空
                        fileName = filename;
                    }
                    else fileName = imgPath.substring(0,imgPath.lastIndexOf("."));
                    //存储图片
                    //获取存储路径
                    String savePath = strRootPath + "/" + fileName + suffix;
                    image.setImgUrl(this.URL +"/images/" + fileName + suffix);
                    if (saveOrUpdateFile(file, savePath, image, 1))
                        result.add("文件更新成功。");
                    else result.add("文件更新失败。");
                }
            }
            //只更新命名
            if (file.isEmpty() && (null != filename&&!filename.trim().equals(""))) {
                File[] files = new File(strRootPath).listFiles();//获取根目录下所有文件
                for (File f : files) {
                    if (f.toString().substring(f.toString().lastIndexOf("/") + 1).equals(imgPath)) {
                        //重命名
                        if (f.renameTo(new File(strRootPath + "/" + filename + image.getImgUrl().substring(image.getImgUrl().lastIndexOf("."))))) {
                            //更新数据库
                            image.setImgUrl(this.URL +"/images/" + filename + image.getImgUrl().substring(image.getImgUrl().lastIndexOf(".")));
                            if (imagesService.updateById(image))
                                result.add("更新成功。");
                            else result.add("更新失败。");
                        }
                        break;
                    }
                }
            }
            //只更新备注
            if(file.isEmpty()&&(null==filename||filename.trim().equals(""))&&null != remark && !remark.trim().equals("")){
                if (imagesService.updateById(image))
                    result.add("更新成功。");
                else result.add("更新失败。");
            }
        } catch (Exception e) {
            result.add("更新失败。");
            session.setAttribute("msgList",result);
        }
        session.setAttribute("msgList",result);
        return "redirect:/images/imageList";
    }

    /**
     * 更新或保存文件
     *
     * @param file     文件
     * @param savePath 保存的路径
     * @param images   要保存的images对象
     * @param saveType 保存的类型，0 保存，1 更新
     * @return
     */
    @Transactional
    public boolean saveOrUpdateFile(MultipartFile file, String savePath, Images images, int saveType) {
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
            if (saveType == 0)//0保持，1更新
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

    /**
     * 删除文件，先获取根目录的所有文件，对比命名删除
     *
     * @param rootPath       根目录
     * @param deleteFileName 要删除的文件名称
     * @return
     */
    public boolean deleteFile(String rootPath, String deleteFileName) {
        File[] files = new File(rootPath).listFiles();//获取根目录下所有文件
        for (File file : files) {
            if (file.toString().substring(file.toString().lastIndexOf("/") + 1).equals(deleteFileName)) {
                file.delete();
                return true;
            }
        }
        return false;
    }


}
