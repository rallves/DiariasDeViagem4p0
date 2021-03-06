SELECT *
FROM (
SELECT AD_ADVTRC.IDINSTPRN IDINSTPRN_NAC,
DT_INI_TRV DT_INI_VIAGEM,
DT_FIM_TRV DT_FIM_VIAGEM,
AD_ADVTRC.CODREGISTRO
FROM AD_ADVTRC
WHERE IDINSTPRN = :IDINSTPRN AND
IDINSTTAR = :IDINSTTAR AND
AD_ADVTRC.CODREGISTRO <> :CODREGISTRO
) TEMP_NAC LEFT JOIN
(
SELECT IDINSTPRN IDINSTPRN_INT, DATINI DT_INI_VIAGEM_INT,
DATFIM DT_FIM_VIAGEM_INT, AD_ADVTRCINT.CODREGISTRO CODREGISTROINT
FROM AD_ADVTRCINT
WHERE IDINSTPRN = :IDINSTPRN AND
IDINSTTAR = :IDINSTTAR AND
AD_ADVTRCINT.CODREGISTRO <> :CODREGISTRO
) TEMP_INT ON (TEMP_NAC.IDINSTPRN_NAC = TEMP_INT.IDINSTPRN_INT)
ORDER BY 1, 2, 3, 4