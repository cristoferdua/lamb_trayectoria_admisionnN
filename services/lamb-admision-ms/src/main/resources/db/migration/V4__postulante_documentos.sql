-- Documentos físicos presentados por el postulante (DNI, certificado, foto, recibos).
CREATE TABLE postulante_documentos (
    id              BIGSERIAL PRIMARY KEY,
    id_postulante   BIGINT       NOT NULL REFERENCES postulantes(id) ON DELETE CASCADE,
    tipo            VARCHAR(30)  NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    observacion     VARCHAR(500),
    fecha_registro  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_postdoc_tipo   CHECK (tipo   IN ('DNI','CERT_COLEGIO','PARTIDA_NACIMIENTO','FOTO','RECIBO_PAGO','OTRO')),
    CONSTRAINT chk_postdoc_estado CHECK (estado IN ('PENDIENTE','ENTREGADO','VALIDADO','OBSERVADO'))
);

CREATE INDEX idx_postdoc_postulante ON postulante_documentos (id_postulante);
