package com.yolov.minio;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Description: minio工具类
 * @ClassName: MinioUtil
 * @Author: 刘苏义
 * @Date: 2023年08月25日14:03:00
 **/
@Slf4j(topic = "minio")
public class MinioUtil {

    private volatile static MinioClient minioClient;
    public static String domainUrl="http://192.168.2.15:9001";
    public static String accessKey="admin";
    public static String secretKey="xzx12345";

    private MinioUtil() {
    }

    public MinioUtil(String domainUrl, String accessKey, String secretKey) {
        this.domainUrl = domainUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        creatMinioClient();
    }

    /**
     * 获取minio客户端实例
     *
     * @return {@link MinioClient}
     */
    public static MinioClient creatMinioClient() {
        if (minioClient == null) {
            synchronized (MinioUtil.class) {
                if (minioClient == null) {
                    minioClient = MinioClient.builder()
                            .endpoint(domainUrl)
                            .credentials(accessKey, secretKey)
                            .build();
                }
            }
        }
        return minioClient;
    }

    /**
     * 判断桶是否存在
     */
    public static boolean exitsBucket(String bucket) {
        boolean found = false;
        try {
            if (StringUtils.isEmpty(bucket)) {
                return false;
            }
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucket).build();
            found = minioClient.bucketExists(bucketExistsArgs);

        } catch (Exception ex) {
            log.error("minio判断桶存在异常：", ex.getMessage());
        }
        return found;
    }

    /**
     * 创建桶
     */
    public static boolean createBucket(String bucket) {
        try {
            if (StringUtils.isEmpty(bucket)) {
                return false;
            }
            //创建桶
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(bucket).build();
            minioClient.makeBucket(makeBucketArgs);
            setBucketPolicy(bucket);//设置桶策略
            setBucketNotificationToMqtt(bucket);//设置桶通知到MQTT
            return true;
        } catch (Exception ex) {
            log.error("minio创建桶异常：", ex.getMessage());
            return false;
        }
    }

    /**
     * 设置桶策略
     * 刘苏义
     * 2023/9/20 10:01:39
     */
    public static boolean setBucketPolicy(String bucket) {
        try {
            //设置策略
            String sb = "{\"Version\":\"2012-10-17\"," +
                    "\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":" +
                    "{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\"," +
                    "\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::" + bucket +
                    "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:PutObject\",\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListMultipartUploadParts\"],\"Resource\":[\"arn:aws:s3:::" +
                    bucket + "/*\"]}]}";
            SetBucketPolicyArgs setBucketPolicyArgs = SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(sb)
                    .build();
            minioClient.setBucketPolicy(setBucketPolicyArgs);
            return true;
        } catch (Exception ex) {
            log.error("设置桶策略异常:" + ex.getMessage());
            return false;
        }
    }

    /**
     * 设置桶通知到MQTT
     * 刘苏义
     * 2023/9/20 9:59:40
     */
    public static boolean setBucketNotificationToMqtt(String bucket) {
        try {
            //设置通知mqtt
            NotificationConfiguration config = new NotificationConfiguration();
            List<QueueConfiguration> queueConfigurations = new ArrayList<>();
            QueueConfiguration queueConfiguration = new QueueConfiguration();
            queueConfiguration.setQueue("arn:minio:sqs::_:mqtt");
            //设置事件
            List<EventType> events = new ArrayList<>();
            events.add(EventType.OBJECT_REMOVED_ANY);
            events.add(EventType.OBJECT_CREATED_ANY);
            queueConfiguration.setEvents(events);
            queueConfigurations.add(queueConfiguration);
            config.setQueueConfigurationList(queueConfigurations);
            SetBucketNotificationArgs setBucketNotificationArgs = SetBucketNotificationArgs.builder()
                    .bucket(bucket)
                    .config(config).build();
            minioClient.setBucketNotification(setBucketNotificationArgs);
            return true;
        } catch (Exception ex) {
            log.error("设置桶通知异常:" + ex.getMessage());
            return false;
        }
    }

    /**
     * 删除一个桶
     *
     * @param bucket 桶名称
     */
    public static boolean removeBucket(String bucket) {
        try {
            boolean found = exitsBucket(bucket);
            if (found) {
                Iterable<Result<Item>> myObjects = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build());
                for (Result<Item> result : myObjects) {
                    Item item = result.get();
                    //有对象文件，则删除失败
                    if (item.size() > 0) {
                        return false;
                    }
                }
                // 删除`bucketName`存储桶，注意，只有存储桶为空时才能删除成功。
                minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
                found = exitsBucket(bucket);
                return !found;
            }
        } catch (Exception ex) {
            log.error("删除桶异常：" + ex.getMessage());
        }
        return false;
    }

    /**
     * 查询所有桶文件
     *
     * @return
     */
    public static List<Bucket> getListBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * 生成一个GET请求的带有失效时间的分享链接。
     * 失效时间默认是7天。
     *
     * @param bucket  存储桶名称
     * @param object  存储桶里的对象名称
     * @param expires 失效时间（以秒为单位），默认是7天，不得大于七天
     * @return
     */
    public static String getObjectWithExpired(String bucket, String object, Integer expires, TimeUnit timeUnit) {
        String url = "";
        if (exitsBucket(bucket)) {
            try {
                GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(object)
                        .expiry(expires, timeUnit)
                        .build();
                url = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
            } catch (Exception ex) {
                log.error("minio生成失效url异常", ex.getMessage());
            }
        } else {
            createBucket(bucket);
        }
        return url;
    }

    /**
     * @描述 上传MultipartFile文件返回url
     * @参数 [bucketName, file]
     * @返回值 java.lang.String
     * @创建人 刘苏义
     * @创建时间 2023/5/18 12:16
     * @修改人和其它信息
     */
    public static String putObjectAndGetUrl(String bucket, MultipartFile file) {
        if (!exitsBucket(bucket)) {
            createBucket(bucket);
        }
        //判断文件是否为空
        if (null == file || 0 == file.getSize()) {
            log.error("上传minio文件服务器错误，上传文件为空");
        }
        //文件名
        String originalFilename = file.getOriginalFilename();
        //新的文件名
        String fileName = UUID.randomUUID() + "_" + originalFilename;
        try {
            InputStream inputStream = file.getInputStream();
            /*上传对象*/
            PutObjectArgs putObjectArgs = PutObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            minioClient.putObject(putObjectArgs);
            inputStream.close();
            /*获取url*/
            GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs
                    .builder()
                    .bucket(bucket)
                    .object(fileName)
                    .method(Method.GET)
                    .build();
            String presignedObjectUrl = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
            String ObjectUrl = presignedObjectUrl.substring(0, presignedObjectUrl.indexOf("?"));
            return ObjectUrl;
        } catch (Exception ex) {
            log.error("上传对象返回url异常：" + ex.getMessage());
        }
        return "";
    }

    /**
     * @描述 上传MultipartFile文件返回url
     * @参数 [bucketName, file]
     * @返回值 java.lang.String
     * @创建人 刘苏义
     * @创建时间 2023/5/18 12:16
     * @修改人和其它信息
     */
    public static String putObjectAndGetUrl(String bucket, String folder, MultipartFile file) {
        if (!exitsBucket(bucket)) {
            createBucket(bucket);
        }
        //判断文件是否为空
        if (null == file || 0 == file.getSize()) {
            log.error("上传minio文件服务器错误，上传文件为空");
        }
        //文件名
        String originalFilename = file.getOriginalFilename();
        //新的文件名
        String fileName = folder + "/" + UUID.randomUUID() + "_" + originalFilename;
        try {
            InputStream inputStream = file.getInputStream();
            /*上传对象*/
            PutObjectArgs putObjectArgs = PutObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            minioClient.putObject(putObjectArgs);
            inputStream.close();
            /*获取url*/
            GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs
                    .builder()
                    .bucket(bucket)
                    .object(fileName)
                    .method(Method.GET)
                    .build();
            String presignedObjectUrl = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
            String ObjectUrl = presignedObjectUrl.substring(0, presignedObjectUrl.indexOf("?"));
            return ObjectUrl;
        } catch (Exception ex) {
            log.error("上传对象返回url异常：" + ex.getMessage());
        }
        return "";
    }

    /**
     * 删除文件
     *
     * @param bucket 桶名称
     * @param object 对象名称
     * @return boolean
     */
    public static boolean removeObject(String bucket, String object) {
        try {
            boolean exsit = exitsBucket(bucket);
            if (exsit) {
                RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(bucket).object(object).build();
                minioClient.removeObject(removeObjectArgs);
                return true;
            }
        } catch (Exception e) {
            log.error("removeObject", e);
        }
        return false;
    }

    /**
     * 批量删除文件
     *
     * @param objectList 对象名称列表
     * @return boolean
     */
    public static boolean removeObjects(String bucket, List<String> objectList) {
        if (exitsBucket(bucket)) {
            try {
                List<DeleteObject> objects = new LinkedList<>();
                for (String str : objectList) {
                    objects.add(new DeleteObject(str));
                }
                RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket).objects(objects).build();
                Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
                /*删除完遍历结果，否则删不掉*/
                for (Result<DeleteError> result : results) {
                    DeleteError error = result.get();
                    log.error("Error in deleting object " + error.objectName() + "; " + error.message());
                }

                return true;
            } catch (Exception ex) {
                log.error("minio批量删除文件异常", ex.getMessage());
            }
        }
        return false;
    }

    /**
     * 获取单个桶中的所有文件对象名称
     *
     * @param bucket 桶名称
     * @return {@link List}<{@link String}>
     */
    public static List<String> getBucketObjectName(String bucket) {
        boolean exsit = exitsBucket(bucket);
        if (exsit) {
            List<String> listObjetcName = new ArrayList<>();
            try {
                ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket(bucket).build();
                Iterable<Result<Item>> myObjects = minioClient.listObjects(listObjectsArgs);
                for (Result<Item> result : myObjects) {
                    Item item = result.get();
                    listObjetcName.add(item.objectName());
                }
                return listObjetcName;
            } catch (Exception ex) {
                log.error("minio获取桶下对象异常：" + ex.getMessage());
            }
        }
        return null;
    }

    /**
     * 获取单个桶中的所有文件对象名称
     *
     * @param bucket 桶名称
     * @param prefix 前缀
     * @return {@link List}<{@link String}>
     */
    public static List<String> getBucketObjectName(String bucket, String prefix) {
        boolean exsit = exitsBucket(bucket);
        if (exsit) {
            List<String> listObjetcName = new ArrayList<>();
            try {
                ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().prefix(prefix).bucket(bucket).build();
                Iterable<Result<Item>> myObjects = minioClient.listObjects(listObjectsArgs);
                for (Result<Item> result : myObjects) {
                    Item item = result.get();
                    listObjetcName.add(item.objectName());
                }
                return listObjetcName;
            } catch (Exception ex) {
                log.error("minio获取桶下对象异常：" + ex.getMessage());
            }
        }
        return null;
    }

    /**
     * 获取某个桶下某个对象的URL
     *
     * @param bucket 桶名称
     * @param object 对象名 (文件夹名 + 文件名)
     * @return
     */
    public static String getBucketObjectUrl(String bucket, String object) {
        try {
            if (!exitsBucket(bucket)) {
                return "";
            }
            GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs
                    .builder()
                    .bucket(bucket)
                    .object(object)
                    .method(Method.GET)
                    .build();
            String presignedObjectUrl = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
            String ObjectUrl = presignedObjectUrl.substring(0, presignedObjectUrl.indexOf("?"));
            return ObjectUrl;
        } catch (Exception ex) {
            log.error("minio获取对象URL异常" + ex.getMessage());
        }
        return "";
    }

    /**
     * 上传对象-stream
     *
     * @param bucket      bucket名称
     * @param object      ⽂件名称
     * @param stream      ⽂件流
     * @param size        ⼤⼩
     * @param contextType 类型 Image/jpeg 浏览器可以直接打开，否则下载
     */
    public static boolean uploadObject(String bucket, String object, InputStream stream, long size, String contextType) {
        try {
            if (!exitsBucket(bucket)) {
                createBucket(bucket);
            }
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .stream(stream, size, -1)
                    .contentType(contextType)
                    .build();
            ObjectWriteResponse objectWriteResponse = minioClient.putObject(putObjectArgs);
            return true;
        } catch (Exception ex) {
            log.error("minio上传文件(通过stream)异常" + ex.getMessage());
            return false;
        }
    }

    /**
     * 上传对象-File
     *
     * @param bucket      bucket名称
     * @param object      ⽂件名称
     * @param file        ⽂件
     * @param contextType 类型 Image/jpeg 浏览器可以直接打开，否则下载
     */
    public static boolean uploadObject(String bucket, String object, File file, String contextType) {
        try {
            if (!exitsBucket(bucket)) {
                createBucket(bucket);
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .stream(fileInputStream, file.length(), -1)
                    .contentType(contextType)
                    .build();
            ObjectWriteResponse objectWriteResponse = minioClient.putObject(putObjectArgs);
            return true;
        } catch (Exception ex) {
            log.error("minio上传文件(通过File)异常" + ex.getMessage());
            return false;
        }
    }

    /**
     * 上传对象-MultipartFile
     *
     * @param bucket        bucket名称
     * @param object        ⽂件名称
     * @param multipartFile ⽂件
     * @param contextType   类型 Image/jpeg 浏览器可以直接打开，否则下载
     */
    public static boolean uploadObject(String bucket, String object, MultipartFile multipartFile, String contextType) {
        try {
            if (!exitsBucket(bucket)) {
                createBucket(bucket);
            }
            InputStream inputStream = multipartFile.getInputStream();
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .stream(inputStream, multipartFile.getSize(), -1)
                    .contentType(contextType)
                    .build();
            ObjectWriteResponse objectWriteResponse = minioClient.putObject(putObjectArgs);
            return true;
        } catch (Exception ex) {
            log.error("minio上传文件(通过File)异常" + ex.getMessage());
            return false;
        }
    }

    /**
     * 上传对象,用multipartFile名称作为对象名
     *
     * @param bucket        bucket名称
     * @param multipartFile ⽂件
     * @param contextType   类型 Image/jpeg 浏览器可以直接打开，否则下载
     */
    public static boolean uploadObject(String bucket, MultipartFile multipartFile, String contextType) {
        try {
            if (!exitsBucket(bucket)) {
                createBucket(bucket);
            }
            if (multipartFile == null) {
                log.error("上传文件为空");
                return false;
            }
            String objectName = multipartFile.getOriginalFilename();
            InputStream inputStream = multipartFile.getInputStream();
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, multipartFile.getSize(), -1)
                    .contentType(contextType)
                    .build();
            minioClient.putObject(putObjectArgs);
            return true;
        } catch (Exception ex) {
            log.error("minio上传文件(通过File)异常" + ex.getMessage());
            return false;
        }
    }

    /**
     * 上传对象-通过本地路径
     *
     * @param bucket            桶名称
     * @param object            对象名称
     * @param localFilePathName 文件路径
     * @return boolean
     */
    public static boolean uploadObject(String bucket, String object, String localFilePathName, String contextType) {
        try {
            if (!exitsBucket(bucket)) {
                createBucket(bucket);
            }
            File file = new File(localFilePathName);
            if (!file.exists()) {
                log.debug("文件不存在");
                return false;
            }
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .filename(localFilePathName)
                    .contentType(contextType)
                    .build();
            ObjectWriteResponse objectWriteResponse = minioClient.uploadObject(uploadObjectArgs);
            return true;
        } catch (Exception e) {
            log.error("minio upload object file error " + e.getMessage());
            return false;
        }
    }

    /**
     * @描述 获取桶中所有对象
     * @参数 [bucketName]
     * @返回值 java.lang.Iterable<io.minio.Result < io.minio.messages.Item>>
     * @创建人 刘苏义
     * @创建时间 2023/2/6 10:32
     * @修改人和其它信息
     */
    public static Iterable<Result<Item>> getObjectsByBucket(String bucketName) {
        Iterable<Result<Item>> listObjects = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build());
        return listObjects;
    }

    /**
     * @描述 获取桶中所有对象
     * @参数 [bucketName, prefix]
     * @返回值 java.lang.Iterable<io.minio.Result < io.minio.messages.Item>>
     * @创建人 刘苏义
     * @创建时间 2023/2/6 10:32
     * @修改人和其它信息
     */
    public static Iterable<Result<Item>> getObjectsByBucket(String bucketName, String prefix) {
        Iterable<Result<Item>> listObjects = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(true)
                .build());
        return listObjects;
    }

}
