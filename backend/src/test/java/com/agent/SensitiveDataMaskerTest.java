package com.agent;

import com.agent.entity.DatasetFieldEntity;
import com.agent.service.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SensitiveDataMasker")
class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void shouldMaskPhoneNumber() {
        assertEquals("联系 *** 咨询", masker.mask("联系 13812345678 咨询", List.of()));
    }

    @Test
    void shouldMaskEmail() {
        String out = masker.mask("邮箱 a.b@corp.com 已发", List.of());
        assertFalse(out.contains("a.b@corp.com"));
    }

    @Test
    void shouldMaskIdCard() {
        String out = masker.mask("身份证 110101199003077777 校验", List.of());
        assertFalse(out.contains("110101199003077777"));
    }

    @Test
    void shouldMaskBankAccount() {
        String out = masker.mask("卡号 6222020200001234567 入账", List.of());
        assertFalse(out.contains("6222020200001234567"));
    }

    @Test
    void shouldMaskSensitiveFieldLabeledValue() {
        DatasetFieldEntity f = new DatasetFieldEntity();
        f.setFieldName("phone");
        f.setFieldAlias("手机号");
        f.setIsSensitive(true);

        String out = masker.mask("手机号: 13812345678", List.of(f));
        assertFalse(out.contains("13812345678"));
        assertTrue(out.contains("手机号"));
    }

    @Test
    void shouldNotMaskNonSensitiveFieldValue() {
        DatasetFieldEntity f = new DatasetFieldEntity();
        f.setFieldName("amount");
        f.setIsSensitive(false);

        // amount value is not sensitive → kept (generic PII formats still masked).
        String out = masker.mask("amount=1000", List.of(f));
        assertTrue(out.contains("1000"));
    }

    @Test
    void shouldKeepNormalTextIntact() {
        assertEquals("华东销售额最高", masker.mask("华东销售额最高", List.of()));
    }
}
