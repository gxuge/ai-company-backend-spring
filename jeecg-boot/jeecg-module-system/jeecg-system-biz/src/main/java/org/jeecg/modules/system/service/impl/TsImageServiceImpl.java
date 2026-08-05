package org.jeecg.modules.system.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.system.dto.tsimage.TsImageDownloadDto;
import org.jeecg.modules.system.service.ITsImageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * TS 通用图片服务实现。
 */
@Service
public class TsImageServiceImpl implements ITsImageService {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;
    private static final int MAX_REDIRECTS = 3;
    private static final long MAX_IMAGE_BYTES = 30L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp"
    );

    /**
     * 校验远程地址并将图片流直接写入响应，不执行图片入库。
     *
     * @param request 下载请求
     * @param response HTTP 响应
     */
    @Override
    public void downloadImage(TsImageDownloadDto request, HttpServletResponse response) {
        HttpURLConnection connection = null;
        try {
            URI sourceUri = URI.create(request.getSourceImageUrl().trim());
            connection = openImageConnection(sourceUri);
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_IMAGE_BYTES) {
                throw new JeecgBootBizTipException("图片大小不能超过30MB");
            }

            try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
                String contentType = resolveContentType(connection, inputStream);
                String fileName = resolveFileName(request.getFileName(), contentType);
                prepareResponse(response, contentType, fileName, contentLength);
                copyImage(inputStream, response.getOutputStream());
            }
        } catch (IllegalArgumentException e) {
            resetResponse(response);
            throw new JeecgBootBizTipException("图片URL格式不正确");
        } catch (JeecgBootBizTipException e) {
            resetResponse(response);
            throw e;
        } catch (IOException e) {
            resetResponse(response);
            throw new JeecgBootBizTipException("图片下载失败，请稍后重试");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 打开远程图片连接，并逐跳校验重定向地址。
     *
     * @param sourceUri 原始图片地址
     * @return 已连接的 HTTP 连接
     * @throws IOException 连接失败
     */
    private HttpURLConnection openImageConnection(URI sourceUri) throws IOException {
        URI currentUri = sourceUri;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            validatePublicHttpUri(currentUri);
            HttpURLConnection connection = (HttpURLConnection) currentUri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "image/*");
            connection.setRequestProperty("User-Agent", "ai-company-ts-image-downloader/1.0");

            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                return connection;
            }
            if (statusCode >= 300 && statusCode < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (!StringUtils.hasText(location) || redirectCount >= MAX_REDIRECTS) {
                    throw new JeecgBootBizTipException("图片地址重定向异常");
                }
                currentUri = currentUri.resolve(location.trim());
                continue;
            }

            connection.disconnect();
            throw new JeecgBootBizTipException("远程图片访问失败，状态码：" + statusCode);
        }
        throw new JeecgBootBizTipException("图片地址重定向次数过多");
    }

    /**
     * 仅允许访问解析到公网地址的 HTTP/HTTPS URL。
     *
     * @param uri 待校验地址
     * @throws IOException 域名解析失败
     */
    private void validatePublicHttpUri(URI uri) throws IOException {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new JeecgBootBizTipException("仅支持HTTP或HTTPS图片地址");
        }
        if (!StringUtils.hasText(uri.getHost()) || uri.getUserInfo() != null) {
            throw new JeecgBootBizTipException("图片URL格式不正确");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (!isPublicAddress(address)) {
                throw new JeecgBootBizTipException("不允许访问内网图片地址");
            }
        }
    }

    /**
     * 判断地址是否属于可访问的公网范围。
     *
     * @param address 已解析地址
     * @return 是否为公网地址
     */
    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            return bytes.length == 16 && (bytes[0] & 0xfe) != 0xfc;
        }
        return bytes.length == 4
                && !((bytes[0] & 0xff) == 100 && ((bytes[1] & 0xc0) == 64));
    }

    /**
     * 结合响应头与文件特征识别图片类型。
     *
     * @param connection 图片连接
     * @param inputStream 支持 mark/reset 的图片流
     * @return 规范化图片类型
     * @throws IOException 流读取失败
     */
    private String resolveContentType(
            HttpURLConnection connection,
            BufferedInputStream inputStream) throws IOException {
        String headerContentType = normalizeContentType(connection.getContentType());
        inputStream.mark(64);
        String detectedContentType = normalizeContentType(URLConnection.guessContentTypeFromStream(inputStream));
        inputStream.reset();

        String contentType = ALLOWED_CONTENT_TYPES.contains(detectedContentType)
                ? detectedContentType
                : headerContentType;
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new JeecgBootBizTipException("远程资源不是支持的图片类型");
        }
        return contentType;
    }

    /**
     * 清理 Content-Type 参数并统一为小写。
     *
     * @param contentType 原始响应类型
     * @return 规范化响应类型
     */
    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        int separatorIndex = contentType.indexOf(';');
        String value = separatorIndex >= 0 ? contentType.substring(0, separatorIndex) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 生成与实际图片类型一致的安全文件名。
     *
     * @param requestedFileName 请求文件名
     * @param contentType 图片类型
     * @return 下载文件名
     */
    private String resolveFileName(String requestedFileName, String contentType) {
        String baseName = StringUtils.hasText(requestedFileName)
                ? requestedFileName.trim()
                : "ts-image-" + System.currentTimeMillis();
        baseName = baseName.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
        baseName = baseName.replaceFirst("(?i)\\.(jpe?g|png|webp|gif|bmp)$", "");
        if (!StringUtils.hasText(baseName)) {
            baseName = "ts-image-" + System.currentTimeMillis();
        }
        return baseName + resolveExtension(contentType);
    }

    /**
     * 根据图片类型返回扩展名。
     *
     * @param contentType 图片类型
     * @return 文件扩展名
     */
    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            default -> ".img";
        };
    }

    /**
     * 设置附件下载响应头。
     *
     * @param response HTTP 响应
     * @param contentType 图片类型
     * @param fileName 下载文件名
     * @param contentLength 图片长度
     */
    private void prepareResponse(
            HttpServletResponse response,
            String contentType,
            String fileName,
            long contentLength) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (contentLength >= 0) {
            response.setContentLengthLong(contentLength);
        }
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    /**
     * 将图片流写入响应并执行大小上限保护。
     *
     * @param inputStream 图片输入流
     * @param outputStream HTTP 输出流
     * @throws IOException 流写入失败
     */
    private void copyImage(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        long totalBytes = 0L;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            totalBytes += read;
            if (totalBytes > MAX_IMAGE_BYTES) {
                throw new JeecgBootBizTipException("图片大小不能超过30MB");
            }
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
    }

    /**
     * 在响应尚未提交时清理已设置的下载响应头。
     *
     * @param response HTTP 响应
     */
    private void resetResponse(HttpServletResponse response) {
        if (!response.isCommitted()) {
            response.reset();
        }
    }
}
