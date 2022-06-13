//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package br.srv.mgs.flow.DiariasDeViagem.EventosDeTabelas;

import br.com.mgs.utils.ErroUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.srv.mgs.commons.NativeSqlDecorator;
import br.srv.mgs.commons.VariaveisFlow;
import com.sankhya.util.TimeUtils;
import kotlin.Triple;
import kotlin.reflect.jvm.internal.impl.types.ErrorUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;


public class CalculoDiaria implements EventoProgramavelJava {
    public CalculoDiaria() {
    }

    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO trechoVO = (DynamicVO)persistenceEvent.getVo();
        BigDecimal codCidDestino = trechoVO.asBigDecimal("CODCID");
        byte[] arqdesps = trechoVO.asBlob("ARQDESP");
        String arquivoHospedagem = null;
        if (arqdesps == null) {
            arquivoHospedagem = new String("N");
        } else {
            arquivoHospedagem = new String("S");
        }

        BigDecimal idInstanciaProcesso = trechoVO.asBigDecimal("IDINSTPRN");
        BigDecimal idInstanciaTarefa = trechoVO.asBigDecimal("IDINSTTAR");
        this.SetarParametrosTrechosNacionais(trechoVO, idInstanciaProcesso, idInstanciaTarefa);
        BigDecimal codTpMun = this.buscaTpMun(codCidDestino);
        String unidadeFaturamento = VariaveisFlow.getVariavel(idInstanciaProcesso, "UNID_FATURAMENTO").toString();


        BigDecimal id_protocolo = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "ID_PROTOCOLO").toString());
        JapeWrapper ad_advdiariaDAO = JapeFactory.dao("AD_ADVDIARIA");
        DynamicVO protocoloVO = ad_advdiariaDAO.findByPK(id_protocolo);
        Timestamp dt_registro = protocoloVO.asTimestamp("DT_REGISTRO");


        BigDecimal ad_numcontrato = null;
        String flagUnidadeFaturamento = unidadeFaturamento.substring(0, 3);
        if (flagUnidadeFaturamento.equals("100")) {
            ad_numcontrato = new BigDecimal(0);
        } else {
            JapeWrapper SiteDAO = JapeFactory.dao("Site");
            DynamicVO SiteFCVO = SiteDAO.findOne("CODSITE = ?", new Object[]{unidadeFaturamento});
            ad_numcontrato = SiteFCVO.asBigDecimal("AD_NUMCONTRATO");
        }

        if (codTpMun == null) {
            throw new Exception("<br><b>A cidade de destino ainda nao na tabela TGFCID!</b><br>");
        } else {
            trechoVO.setProperty("CODTPMUN", codTpMun);
            String codLim = trechoVO.asString("CIDLIM");
            String convTerc = trechoVO.asString("CONVTERC");
            String comproHospedagem = trechoVO.asString("COMPRHOSP");
            String faixaEnquadramentoObj = VariaveisFlow.getVariavel(idInstanciaProcesso, "COD_FAIXA").toString();
            String faixaEnquadramento = null;
            if (faixaEnquadramentoObj == null) {
                throw new Exception("<br><b>A faixa de enquadramento deve ser preenchida!</b><br>");
            } else {
                faixaEnquadramento = faixaEnquadramentoObj.toString();
                String dataInicioTrecho = new SimpleDateFormat("dd/MM/yyyy").format(trechoVO.asTimestamp("DT_INI_TRV"));
                JapeWrapper tabVlrFaixaDAO = JapeFactory.dao("AD_ADVVLRFAIXA");
                DynamicVO tabVlrFaixaVO = tabVlrFaixaDAO.findOne("CODFAIXA = ? and CODTPMUN = ? and NUMCONTRATO = ? and DTINI <= ? and DTFIM >= ?", new BigDecimal(faixaEnquadramento), codTpMun, ad_numcontrato, dataInicioTrecho, dataInicioTrecho);
                if (tabVlrFaixaVO == null) {
                    throw new Exception("<br><b>Não foram encontrados valores de diarias de viagem para o contrato " + ad_numcontrato + " </b><br>");
                } else if (tabVlrFaixaVO == null) {
                    throw new Exception("<br><b>A faixa de enquadramento deve ser preenchida!</b><br>");
                } else {
                    BigDecimal vlrIntegral = tabVlrFaixaVO.asBigDecimal("VLRINTEGRAL");
                    Timestamp dtFimTrv = trechoVO.asTimestamp("DT_FIM_TRV");
                    Timestamp dtIniTrv = trechoVO.asTimestamp("DT_INI_TRV");
                    if (dtFimTrv.compareTo(dtIniTrv) < 0) {
                        throw new Exception("<br><b>A data do inicio do trecho deve ser maior que a data fim do trecho!</b><br>");
                    } else if (this.BuscarTrechosConflitantes(persistenceEvent)) {
                        throw new Exception("<br><b>Conflito de Periodos encontrado. Verifique os trechos já gravados</b><br>");
                    } else {
                        long differenceInHour = TimeUtils.getDifferenceInHour(dtFimTrv, dtIniTrv);
                        String diaFim = (new SimpleDateFormat("dd")).format(dtFimTrv.getTime());
                        String diaInicio = (new SimpleDateFormat("dd")).format(dtIniTrv.getTime());
                        BigDecimal difDias = BigDecimal.ZERO;
                        Integer fracaoDiaria = null;
                        Integer fracaoInteiraTrecho = Math.toIntExact(differenceInHour / 24L);
                        Double fracaoTrecho = Double.valueOf((double)TimeUtils.getDifferenceInHour(dtFimTrv, dtIniTrv)) / 24.0D - (double)Math.toIntExact(differenceInHour / 24L);
                        if ((new BigDecimal(diaFim)).compareTo(new BigDecimal(diaInicio)) > 0) {
                            difDias = new BigDecimal(1);
                        }

                        BigDecimal fracao = BigDecimal.ZERO;
                        new Integer(0);
                        NativeSqlDecorator nativeSqlDecoratorFracaoDV = new NativeSqlDecorator(this, "sql/BuscarFracaoDiaria.sql");
                        nativeSqlDecoratorFracaoDV.setParametro("FRACAO", fracaoTrecho);
                        nativeSqlDecoratorFracaoDV.setParametro("COMPRHOSP", comproHospedagem);
                        nativeSqlDecoratorFracaoDV.setParametro("CIDLIMITR", codLim);
                        nativeSqlDecoratorFracaoDV.setParametro("CUSTERC", convTerc);
                        nativeSqlDecoratorFracaoDV.setParametro("DIFDIAS", difDias);
                        boolean proximo = nativeSqlDecoratorFracaoDV.proximo();
                        if (proximo) {
                            fracao = nativeSqlDecoratorFracaoDV.getValorBigDecimal("FRACADV");
                            if (difDias.compareTo(new BigDecimal(1)) == 0 && (new BigDecimal(differenceInHour)).compareTo(new BigDecimal(24)) < 0) {
                                fracaoInteiraTrecho = new Integer(0);
                            } else {
                                fracaoInteiraTrecho = Math.toIntExact(differenceInHour / 24L);
                            }

                            fracao = (new BigDecimal(fracaoInteiraTrecho)).add(new BigDecimal(String.valueOf(fracao)));
                        }

                        trechoVO.setProperty("VALOR", fracao.multiply(vlrIntegral));
                        BigDecimal codtransp = trechoVO.asBigDecimal("CODTRANSP");
                        JapeWrapper tabTpTransporteDAO = JapeFactory.dao("AD_ADVTPTRANSP");
                        DynamicVO tabTpTransporteVO = tabTpTransporteDAO.findByPK(new Object[]{codtransp});
                        String descrtransp = tabTpTransporteVO.asString("DESCRTRANSP");
                        Object descr_transporte_viagemObj = VariaveisFlow.getVariavel(idInstanciaProcesso, "DESCR_TRANSPORTE_VIAGEM");
                        String descr_transporte_viagem = null;
                        if (descr_transporte_viagemObj == null) {
                            descr_transporte_viagem = descrtransp;
                        } else if (descr_transporte_viagemObj.toString() == descrtransp) {
                            descr_transporte_viagem = descr_transporte_viagemObj.toString();
                        } else {
                            descr_transporte_viagem = descr_transporte_viagemObj.toString() + descrtransp;
                        }

                        NativeSqlDecorator nativeSqlDecoratorComprovante = new NativeSqlDecorator(this, "sql/BuscarComprovanteHospedagem.sql");
                        nativeSqlDecoratorComprovante.setParametro("IDINSTPRN", idInstanciaProcesso);
                        nativeSqlDecoratorComprovante.setParametro("IDINSTTAR", idInstanciaTarefa);
                        boolean proximoCupomDespesa = nativeSqlDecoratorComprovante.proximo();
                        if (!proximoCupomDespesa && !arquivoHospedagem.equals(new String("S"))) {
                            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "COMPHOSP", "N");
                        } else {
                            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "COMPHOSP", "S");
                        }

                        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PREV_DIARIAS", fracao);
                        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DESCR_TRANSPORTE_VIAGEM", descr_transporte_viagem);
                    }
                }
            }
        }
    }

    private BigDecimal getFracaoDiaria(String codLim, String convTerc, String comproHospedagem, long differenceInHour) throws Exception {
        JapeWrapper tabRatPercDAO = JapeFactory.dao("AD_ADVRATVLR");
        BigDecimal percentual = BigDecimal.ZERO;
        Triple chavePesquisa = this.getChavePesquisa(new BigDecimal(differenceInHour));
        DynamicVO percVO;
        if (chavePesquisa.component1() != null) {
            percVO = tabRatPercDAO.findOne("DIFHORAS = ? AND COMPRHOSP = ? AND CIDLIMITR = ? AND CUSTERC = ?", new Object[]{chavePesquisa.component1(), comproHospedagem, codLim, convTerc});
            if (percVO != null) {
                percentual = percentual.add(percVO.asBigDecimal("FRACADV").multiply((BigDecimal)chavePesquisa.component3()));
            }
        }

        if (chavePesquisa.component2() != null) {
            percVO = tabRatPercDAO.findOne("DIFHORAS = ? AND COMPRHOSP = ? AND CIDLIMITR = ? AND CUSTERC = ?", new Object[]{chavePesquisa.component2(), comproHospedagem, codLim, convTerc});
            if (percVO != null) {
                percentual = percentual.add(percVO.asBigDecimal("FRACADV"));
            }
        }

        return percentual;
    }

    private Triple getChavePesquisa(BigDecimal diferenceHour) {
        BigDecimal dia = new BigDecimal(24);
        BigDecimal[] dias = diferenceHour.divideAndRemainder(dia, new MathContext(2, RoundingMode.HALF_EVEN));
        return new Triple(this.getChaveValor(dias[0]), this.getChaveValor(dias[1]), dias[0]);
    }

    private String getChaveValor(BigDecimal dias) {
        if (dias.compareTo(new BigDecimal(0.25D)) >= 0 && dias.compareTo(new BigDecimal(0.5D)) < 0) {
            return "00,25-00,50";
        } else {
            return dias.compareTo(new BigDecimal(0.5D)) >= 0 ? "00,50-01,00" : null;
        }
    }

    private BigDecimal buscaTpMun(BigDecimal cidade) throws Exception {
        JapeWrapper cidadeDAO = JapeFactory.dao("Cidade");
        DynamicVO cidadeVO = cidadeDAO.findByPK(new Object[]{cidade});
        return cidadeVO.asBigDecimal("AD_CODTPMUN");
    }

    public void SetarParametrosTrechosNacionais(DynamicVO trechoForm, BigDecimal idInstanciaProcesso, BigDecimal idInstanciaTarefa) throws Exception {
        JapeWrapper advparams = JapeFactory.dao("AD_ADVPARAMS");
        DynamicVO advparamsVO = advparams.findByPK(new Object[]{new BigDecimal(1)});
        String cidlim = advparamsVO.asString("CIDLIM");
        String comhosp = advparamsVO.asString("COMHOSP");
        String convtrc = advparamsVO.asString("CONVTRC");
        BigDecimal codtransp = advparamsVO.asBigDecimal("CODTRANSP");
        Object comprhospedagemVO = trechoForm.asString("COMPRHOSP");
        Object cidlimVO = trechoForm.asString("CIDLIM");
        Object convtercVO = trechoForm.asString("CONVTERC");
        Object codtranspVO = trechoForm.asBigDecimal("CODTRANSP");
        if (codtranspVO == null) {
            trechoForm.setProperty("CODTRANSP", new BigDecimal(String.valueOf(codtransp)));
        }

        if (comprhospedagemVO == null) {
            trechoForm.setProperty("COMPRHOSP", String.valueOf(comhosp));
        }

        if (cidlimVO == null) {
            trechoForm.setProperty("CIDLIM", String.valueOf(cidlim));
        }

        if (convtercVO == null) {
            trechoForm.setProperty("CONVTERC", String.valueOf(convtrc));
        }

    }

    public Boolean BuscarTrechosConflitantes(PersistenceEvent persistenceEvent) throws Exception {
        //teste dia 09/06/2022
        DynamicVO trechoVO = (DynamicVO)persistenceEvent.getVo();
        BigDecimal idInstanciaProcesso = trechoVO.asBigDecimal("IDINSTPRN");
        BigDecimal idInstanciaTarefa = trechoVO.asBigDecimal("IDINSTTAR");
        Timestamp dataInicioViagemForm = trechoVO.asTimestamp("DT_INI_TRV");
        Timestamp dataFimViagemForm = trechoVO.asTimestamp("DT_FIM_TRV");
        BigDecimal codregistroForm = trechoVO.asBigDecimal("CODREGISTRO");
        NativeSqlDecorator nativeSqlDecorator = new NativeSqlDecorator(this, "sql/BuscarTrechosRegistrados.sql");
        nativeSqlDecorator.setParametro("IDINSTPRN", idInstanciaProcesso);
        nativeSqlDecorator.setParametro("IDINSTTAR", idInstanciaTarefa);
        nativeSqlDecorator.setParametro("CODREGISTRO", codregistroForm);

        for(boolean proximo = nativeSqlDecorator.proximo(); proximo; proximo = nativeSqlDecorator.proximo()) {
            Timestamp dataInicioViagem = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM");
            Timestamp dataFimViagem = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM");
            Timestamp dataInicioViagemInternacional = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM_INT");
            Timestamp dataFimViagemInternacional = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM_INT");
            if (dataInicioViagem != null && dataFimViagem != null) {
                if (dataInicioViagemForm.compareTo(dataInicioViagem) >= 0 && dataInicioViagemForm.compareTo(dataFimViagem) <= 0 || dataFimViagemForm.compareTo(dataInicioViagem) >= 0 && dataFimViagemForm.compareTo(dataFimViagem) <= 0) {
                    return true;
                }

                if (dataInicioViagem.compareTo(dataInicioViagemForm) >= 0 && dataInicioViagem.compareTo(dataFimViagemForm) <= 0 || dataFimViagem.compareTo(dataInicioViagemForm) >= 0 && dataFimViagem.compareTo(dataFimViagemForm) <= 0) {
                    return true;
                }
            }

            if (dataInicioViagemInternacional != null && dataFimViagemInternacional != null && (dataInicioViagemForm.compareTo(dataInicioViagemInternacional) >= 0 && dataInicioViagemForm.compareTo(dataFimViagemInternacional) <= 0 || dataFimViagemForm.compareTo(dataFimViagemInternacional) <= 0 && dataFimViagem.compareTo(dataInicioViagemInternacional) >= 0)) {
                return true;
            }
        }

        return false;
    }

    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {
        //teste de inclusão no github
        DynamicVO trechoVO = (DynamicVO)persistenceEvent.getVo();
        BigDecimal codCidDestino = trechoVO.asBigDecimal("CODCID");
        byte[] arqdesps = trechoVO.asBlob("ARQDESP");
        String arquivoHospedagem = null;
        if (arqdesps == null) {
            arquivoHospedagem = new String("N");
        } else {
            arquivoHospedagem = new String("S");
        }

        BigDecimal idInstanciaProcesso = trechoVO.asBigDecimal("IDINSTPRN");
        BigDecimal idInstanciaTarefa = trechoVO.asBigDecimal("IDINSTTAR");
        this.SetarParametrosTrechosNacionais(trechoVO, idInstanciaProcesso, idInstanciaTarefa);
        BigDecimal codTpMun = this.buscaTpMun(codCidDestino);
        String unidadeFaturamento = VariaveisFlow.getVariavel(idInstanciaProcesso, "UNID_FATURAMENTO").toString();


        BigDecimal id_protocolo = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "ID_PROTOCOLO").toString());
        JapeWrapper ad_advdiariaDAO = JapeFactory.dao("AD_ADVDIARIA");
        DynamicVO protocoloVO = ad_advdiariaDAO.findByPK(id_protocolo);
        Timestamp dt_registro = protocoloVO.asTimestamp("DT_REGISTRO");


        BigDecimal ad_numcontrato = null;
        String flagUnidadeFaturamento = unidadeFaturamento.substring(0, 3);
        if (flagUnidadeFaturamento.equals("100")) {
            ad_numcontrato = new BigDecimal(0);
        } else {
            JapeWrapper SiteDAO = JapeFactory.dao("Site");
            DynamicVO SiteFCVO = SiteDAO.findOne("CODSITE = ?", new Object[]{unidadeFaturamento});
            ad_numcontrato = SiteFCVO.asBigDecimal("AD_NUMCONTRATO");
        }

        if (codTpMun == null) {
            throw new Exception("<br><b>A cidade de destino ainda nao na tabela TGFCID!</b><br>");
        } else {
            trechoVO.setProperty("CODTPMUN", codTpMun);
            String codLim = trechoVO.asString("CIDLIM");
            String convTerc = trechoVO.asString("CONVTERC");
            String comproHospedagem = trechoVO.asString("COMPRHOSP");
            String faixaEnquadramentoObj = VariaveisFlow.getVariavel(idInstanciaProcesso, "COD_FAIXA").toString();
            String faixaEnquadramento = null;
            if (faixaEnquadramentoObj == null) {
                throw new Exception("<br><b>A faixa de enquadramento deve ser preenchida!</b><br>");
            } else {
                faixaEnquadramento = faixaEnquadramentoObj.toString();
                String dataInicioTrecho = new SimpleDateFormat("dd/MM/yyyy").format(trechoVO.asTimestamp("DT_INI_TRV"));
                JapeWrapper tabVlrFaixaDAO = JapeFactory.dao("AD_ADVVLRFAIXA");
                DynamicVO tabVlrFaixaVO = tabVlrFaixaDAO.findOne("CODFAIXA = ? and CODTPMUN = ? and NUMCONTRATO = ? and DTINI <= ? and DTFIM >= ?", new BigDecimal(faixaEnquadramento), codTpMun, ad_numcontrato, dataInicioTrecho, dataInicioTrecho);
                if (tabVlrFaixaVO == null) {
                    throw new Exception("<br><b>Não foram encontrados valores de diarias de viagem para o contrato " + ad_numcontrato + " </b><br>");
                } else if (tabVlrFaixaVO == null) {
                    throw new Exception("<br><b>A faixa de enquadramento deve ser preenchida!</b><br>");
                } else {
                    BigDecimal vlrIntegral = tabVlrFaixaVO.asBigDecimal("VLRINTEGRAL");
                    Timestamp dtFimTrv = trechoVO.asTimestamp("DT_FIM_TRV");
                    Timestamp dtIniTrv = trechoVO.asTimestamp("DT_INI_TRV");
                    if (dtFimTrv.compareTo(dtIniTrv) < 0) {
                        throw new Exception("<br><b>A data do inicio do trecho deve ser maior que a data fim do trecho!</b><br>");
                    } else if (this.BuscarTrechosConflitantes(persistenceEvent)) {
                        throw new Exception("<br><b>Conflito de Periodos encontrado. Verifique os trechos já gravados</b><br>");
                    } else {
                        long differenceInHour = TimeUtils.getDifferenceInHour(dtFimTrv, dtIniTrv);
                        String diaFim = (new SimpleDateFormat("dd")).format(dtFimTrv.getTime());
                        String diaInicio = (new SimpleDateFormat("dd")).format(dtIniTrv.getTime());
                        BigDecimal difDias = BigDecimal.ZERO;
                        Integer fracaoDiaria = null;
                        Integer fracaoInteiraTrecho = Math.toIntExact(differenceInHour / 24L);
                        Double fracaoTrecho = Double.valueOf((double)TimeUtils.getDifferenceInHour(dtFimTrv, dtIniTrv)) / 24.0D - (double)Math.toIntExact(differenceInHour / 24L);
                        if ((new BigDecimal(diaFim)).compareTo(new BigDecimal(diaInicio)) > 0) {
                            difDias = new BigDecimal(1);
                        }

                        BigDecimal fracao = BigDecimal.ZERO;
                        new Integer(0);
                        NativeSqlDecorator nativeSqlDecoratorFracaoDV = new NativeSqlDecorator(this, "sql/BuscarFracaoDiaria.sql");
                        nativeSqlDecoratorFracaoDV.setParametro("FRACAO", fracaoTrecho);
                        nativeSqlDecoratorFracaoDV.setParametro("COMPRHOSP", comproHospedagem);
                        nativeSqlDecoratorFracaoDV.setParametro("CIDLIMITR", codLim);
                        nativeSqlDecoratorFracaoDV.setParametro("CUSTERC", convTerc);
                        nativeSqlDecoratorFracaoDV.setParametro("DIFDIAS", difDias);
                        boolean proximo = nativeSqlDecoratorFracaoDV.proximo();
                        if (proximo) {
                            fracao = nativeSqlDecoratorFracaoDV.getValorBigDecimal("FRACADV");
                            if (difDias.compareTo(new BigDecimal(1)) == 0 && (new BigDecimal(differenceInHour)).compareTo(new BigDecimal(24)) < 0) {
                                fracaoInteiraTrecho = new Integer(0);
                            } else {
                                fracaoInteiraTrecho = Math.toIntExact(differenceInHour / 24L);
                            }

                            fracao = (new BigDecimal(fracaoInteiraTrecho)).add(new BigDecimal(String.valueOf(fracao)));
                        }

                        trechoVO.setProperty("VALOR", fracao.multiply(vlrIntegral));
                        BigDecimal codtransp = trechoVO.asBigDecimal("CODTRANSP");
                        JapeWrapper tabTpTransporteDAO = JapeFactory.dao("AD_ADVTPTRANSP");
                        DynamicVO tabTpTransporteVO = tabTpTransporteDAO.findByPK(new Object[]{codtransp});
                        String descrtransp = tabTpTransporteVO.asString("DESCRTRANSP");
                        Object descr_transporte_viagemObj = VariaveisFlow.getVariavel(idInstanciaProcesso, "DESCR_TRANSPORTE_VIAGEM");
                        String descr_transporte_viagem = null;
                        if (descr_transporte_viagemObj == null) {
                            descr_transporte_viagem = descrtransp;
                        } else if (descr_transporte_viagemObj.toString() == descrtransp) {
                            descr_transporte_viagem = descr_transporte_viagemObj.toString();
                        } else {
                            descr_transporte_viagem = descr_transporte_viagemObj.toString() + descrtransp;
                        }

                        NativeSqlDecorator nativeSqlDecoratorComprovante = new NativeSqlDecorator(this, "sql/BuscarComprovanteHospedagem.sql");
                        nativeSqlDecoratorComprovante.setParametro("IDINSTPRN", idInstanciaProcesso);
                        nativeSqlDecoratorComprovante.setParametro("IDINSTTAR", idInstanciaTarefa);
                        boolean proximoCupomDespesa = nativeSqlDecoratorComprovante.proximo();
                        if (!proximoCupomDespesa && !arquivoHospedagem.equals(new String("S"))) {
                            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "COMPHOSP", "N");
                        } else {
                            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "COMPHOSP", "S");
                        }

                        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PREV_DIARIAS", fracao);
                        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DESCR_TRANSPORTE_VIAGEM", descr_transporte_viagem);
                    }
                }
            }
        }
    }

    public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO AD_ADVTRC = (DynamicVO) persistenceEvent.getVo();
        Timestamp idinstprn = AD_ADVTRC.asTimestamp("IDINSTPRN");

        NativeSqlDecorator nativeSqlDecoratorTarefa = new NativeSqlDecorator(this, "sql/BuscarTarefaAtiva.sql");
        nativeSqlDecoratorTarefa.setParametro("IDINSTPRN", idinstprn);
        if (nativeSqlDecoratorTarefa.proximo()){
            if (nativeSqlDecoratorTarefa.getValorString("IDELEMENTO").compareTo("UserTask_0y596wb")== 0){
                ErroUtils.disparaErro("Ação não permitida. Registro já está em fase de aprovação!");
            }
        }
    }

    public void afterInsert(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO despesaVO = (DynamicVO)persistenceEvent.getVo();
        BigDecimal idInstanciaProcesso = despesaVO.asBigDecimal("IDINSTPRN");
        BigDecimal idInstanciaTarefa = despesaVO.asBigDecimal("IDINSTTAR");
        NativeSqlDecorator nativeSqlDecorator = new NativeSqlDecorator(this, "sql/BuscarPeriodosViagem.sql");
        nativeSqlDecorator.setParametro("IDINSTPRN", idInstanciaProcesso);
        nativeSqlDecorator.setParametro("IDINSTTAR", idInstanciaTarefa);
        String cargaReg = (String)VariaveisFlow.getVariavel(idInstanciaProcesso, "CARGA_REG");
        if (cargaReg == null) {
            boolean proximo = nativeSqlDecorator.proximo();
            Timestamp dt_ini_viagem = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM");
            Timestamp dt_fim_viagem = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM");
            Timestamp dt_ini_viagem_int = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM_INT");
            Timestamp dt_fim_viagem_int = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM_INT");
            Timestamp dataInicioViagem;
            Timestamp dataFimViagem;
            if (dt_ini_viagem != null && dt_ini_viagem_int != null) {
                if (dt_ini_viagem.compareTo(dt_ini_viagem_int) < 0) {
                    dataInicioViagem = dt_ini_viagem;
                } else {
                    dataInicioViagem = dt_ini_viagem_int;
                }

                if (dt_fim_viagem.compareTo(dt_fim_viagem_int) > 0) {
                    dataFimViagem = dt_fim_viagem;
                } else {
                    dataFimViagem = dt_fim_viagem_int;
                }
            } else if (dt_ini_viagem != null) {
                dataInicioViagem = dt_ini_viagem;
                dataFimViagem = dt_fim_viagem;
            } else {
                dataInicioViagem = dt_ini_viagem_int;
                dataFimViagem = dt_fim_viagem_int;
            }

            Object tp_registroObject = VariaveisFlow.getVariavel(idInstanciaProcesso, "TP_REGISTRO");
            String tp_registro;
            if (tp_registroObject == null) {
                tp_registro = "1";
            } else {
                tp_registro = tp_registroObject.toString();
            }

            if (tp_registro.intern() == "1") {
                VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_INICIO_VIAGEM", dataInicioViagem);
                VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_TERMINO_VIAGEM", dataFimViagem);
            } else {
                VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_INICIO_VIAGEM", dataInicioViagem);
                VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_TERMINO_VIAGEM", dataFimViagem);
                VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_REAL_INICIO_VIAGEM", dataInicioViagem);
                VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_REAL_FINAL_VIAGEM", dataFimViagem);
            }
        }

    }

    public void afterUpdate(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO despesaVO = (DynamicVO)persistenceEvent.getVo();
        BigDecimal idInstanciaProcesso = despesaVO.asBigDecimal("IDINSTPRN");
        BigDecimal idInstanciaTarefa = despesaVO.asBigDecimal("IDINSTTAR");
        NativeSqlDecorator nativeSqlDecorator = new NativeSqlDecorator(this, "sql/BuscarPeriodosViagem.sql");
        nativeSqlDecorator.setParametro("IDINSTPRN", idInstanciaProcesso);
        nativeSqlDecorator.setParametro("IDINSTTAR", idInstanciaTarefa);
        boolean proximo = nativeSqlDecorator.proximo();
        Timestamp dt_ini_viagem = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM");
        Timestamp dt_fim_viagem = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM");
        Timestamp dt_ini_viagem_int = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM_INT");
        Timestamp dt_fim_viagem_int = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM_INT");
        Timestamp dataInicioViagem;
        Timestamp dataFimViagem;
        if (dt_ini_viagem != null && dt_ini_viagem_int != null) {
            if (dt_ini_viagem.compareTo(dt_ini_viagem_int) < 0) {
                dataInicioViagem = dt_ini_viagem;
            } else {
                dataInicioViagem = dt_ini_viagem_int;
            }

            if (dt_fim_viagem.compareTo(dt_fim_viagem_int) > 0) {
                dataFimViagem = dt_fim_viagem;
            } else {
                dataFimViagem = dt_fim_viagem_int;
            }
        } else if (dt_ini_viagem != null) {
            dataInicioViagem = dt_ini_viagem;
            dataFimViagem = dt_fim_viagem;
        } else {
            dataInicioViagem = dt_ini_viagem_int;
            dataFimViagem = dt_fim_viagem_int;
        }

        Object tpRegistroObj = VariaveisFlow.getVariavel(idInstanciaProcesso, "TP_REGISTRO");
        String tp_registro = null;
        if (tpRegistroObj == null) {
            tp_registro = "1";
        } else {
            tp_registro = tpRegistroObj.toString();
        }

        if (tp_registro.intern() == "1") {
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_INICIO_VIAGEM", dataInicioViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_TERMINO_VIAGEM", dataFimViagem);
        } else {
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_INICIO_VIAGEM", dataInicioViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_TERMINO_VIAGEM", dataFimViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_REAL_INICIO_VIAGEM", dataInicioViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_REAL_FINAL_VIAGEM", dataFimViagem);
        }

    }

    public void afterDelete(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO despesaVO = (DynamicVO)persistenceEvent.getVo();
        BigDecimal idInstanciaProcesso = despesaVO.asBigDecimal("IDINSTPRN");
        BigDecimal idInstanciaTarefa = despesaVO.asBigDecimal("IDINSTTAR");
        NativeSqlDecorator nativeSqlDecorator = new NativeSqlDecorator(this, "sql/BuscarPeriodosViagem.sql");
        nativeSqlDecorator.setParametro("IDINSTPRN", idInstanciaProcesso);
        nativeSqlDecorator.setParametro("IDINSTTAR", idInstanciaTarefa);
        boolean proximo = nativeSqlDecorator.proximo();
        Timestamp dt_ini_viagem = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM");
        Timestamp dt_fim_viagem = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM");
        Timestamp dt_ini_viagem_int = nativeSqlDecorator.getValorTimestamp("DT_INI_VIAGEM_INT");
        Timestamp dt_fim_viagem_int = nativeSqlDecorator.getValorTimestamp("DT_FIM_VIAGEM_INT");
        Timestamp dataInicioViagem;
        Timestamp dataFimViagem;
        if (dt_ini_viagem != null && dt_ini_viagem_int != null) {
            if (dt_ini_viagem.compareTo(dt_ini_viagem_int) < 0) {
                dataInicioViagem = dt_ini_viagem;
            } else {
                dataInicioViagem = dt_ini_viagem_int;
            }

            if (dt_fim_viagem.compareTo(dt_fim_viagem_int) > 0) {
                dataFimViagem = dt_fim_viagem;
            } else {
                dataFimViagem = dt_fim_viagem_int;
            }
        } else if (dt_ini_viagem != null) {
            dataInicioViagem = dt_ini_viagem;
            dataFimViagem = dt_fim_viagem;
        } else {
            dataInicioViagem = dt_ini_viagem_int;
            dataFimViagem = dt_fim_viagem_int;
        }

        Object tp_registroObj = VariaveisFlow.getVariavel(idInstanciaProcesso, "TP_REGISTRO");
        String tp_registro = null;
        if (tp_registroObj == null) {
            tp_registro = "1";
        } else {
            tp_registro = tp_registroObj.toString();
        }

        if (tp_registro.intern() == "1") {
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_INICIO_VIAGEM", dataInicioViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_TERMINO_VIAGEM", dataFimViagem);
        } else {
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_INICIO_VIAGEM", dataInicioViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_TERMINO_VIAGEM", dataFimViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_REAL_INICIO_VIAGEM", dataInicioViagem);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "DAT_REAL_FINAL_VIAGEM", dataFimViagem);
        }

    }

    public void beforeCommit(TransactionContext transactionContext) throws Exception {
    }
}
