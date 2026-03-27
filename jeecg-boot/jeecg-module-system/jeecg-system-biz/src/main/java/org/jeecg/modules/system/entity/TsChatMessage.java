package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_chat_message")
public class TsChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("session_id")
    private Long sessionId;
    @TableField("sender_type")
    private String senderType;
    @TableField("sender_id")
    private Long senderId;
    @TableField("sender_name")
    private String senderName;
    @TableField("message_type")
    private String messageType;
    @TableField("content_text")
    private String contentText;
    @TableField("content_json")
    private String contentJson;
    @TableField("reply_to_message_id")
    private Long replyToMessageId;
    @TableField("seq_no")
    private Long seqNo;
    @TableField("generate_status")
    private String generateStatus;
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
