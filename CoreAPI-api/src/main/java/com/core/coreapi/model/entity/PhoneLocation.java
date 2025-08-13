package com.core.coreapi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 手机号归属地
 */
@TableName(value ="phone_location")
@Data
@Schema(description = "手机号归属地")
public class PhoneLocation implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "82141")
    private Integer id;

    /**
     * 号段前缀
     */
    @Schema(description = "号段前缀", example = "138")
    private String pref;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "1381995")
    private String phone;

    /**
     * 省份
     */
    @Schema(description = "省份", example = "浙江")
    private String province;

    /**
     * 城市
     */
    @Schema(description = "城市", example = "金华")
    private String city;

    /**
     * 运营商类型名称
     */
    @Schema(description = "运营商类型名称", example = "中国移动")
    private String isp;

    /**
     * 运营商类型（1：移动 2：联通 3：电信 4：广电 5：工信）
     */
    @Schema(description = "运营商类型（1：移动 2：联通 3：电信 4：广电 5：工信）", example = "1")
    private Integer ispType;

    /**
     * 邮政编码
     */
    @Schema(description = "邮政编码", example = "321000")
    private String postCode;

    /**
     * 城市区号
     */
    @Schema(description = "城市区号", example = "0579")
    private String cityCode;

    /**
     * 行政区划编码
     */
    @Schema(description = "行政区划编码", example = "330700")
    private String areaCode;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-03-04T06:50:26.000+00:00")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        PhoneLocation other = (PhoneLocation) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getPref() == null ? other.getPref() == null : this.getPref().equals(other.getPref()))
            && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
            && (this.getProvince() == null ? other.getProvince() == null : this.getProvince().equals(other.getProvince()))
            && (this.getCity() == null ? other.getCity() == null : this.getCity().equals(other.getCity()))
            && (this.getIsp() == null ? other.getIsp() == null : this.getIsp().equals(other.getIsp()))
            && (this.getIspType() == null ? other.getIspType() == null : this.getIspType().equals(other.getIspType()))
            && (this.getPostCode() == null ? other.getPostCode() == null : this.getPostCode().equals(other.getPostCode()))
            && (this.getCityCode() == null ? other.getCityCode() == null : this.getCityCode().equals(other.getCityCode()))
            && (this.getAreaCode() == null ? other.getAreaCode() == null : this.getAreaCode().equals(other.getAreaCode()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getPref() == null) ? 0 : getPref().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getProvince() == null) ? 0 : getProvince().hashCode());
        result = prime * result + ((getCity() == null) ? 0 : getCity().hashCode());
        result = prime * result + ((getIsp() == null) ? 0 : getIsp().hashCode());
        result = prime * result + ((getIspType() == null) ? 0 : getIspType().hashCode());
        result = prime * result + ((getPostCode() == null) ? 0 : getPostCode().hashCode());
        result = prime * result + ((getCityCode() == null) ? 0 : getCityCode().hashCode());
        result = prime * result + ((getAreaCode() == null) ? 0 : getAreaCode().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", pref=").append(pref);
        sb.append(", phone=").append(phone);
        sb.append(", province=").append(province);
        sb.append(", city=").append(city);
        sb.append(", isp=").append(isp);
        sb.append(", ispType=").append(ispType);
        sb.append(", postCode=").append(postCode);
        sb.append(", cityCode=").append(cityCode);
        sb.append(", areaCode=").append(areaCode);
        sb.append(", createTime=").append(createTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}