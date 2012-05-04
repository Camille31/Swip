#!/usr/bin/env python -O
# -*- coding: iso-8859-15 -*-
#

#################
# LES CLITIQUES #
#################

# Liste des clitiques post-verbaux que l'on peut rencontrer en francais
clitiques_post_verbaux=["je","tu","il","elle","on","nous","vous","ils","elles","moi","toi","lui","leur","le","la","les","y","en"]
clitiques_post_verbaux.extend(["-je","-tu","-il","-elle","-on","-nous","-vous","-ils","-elles","-moi","-toi","-lui","-leur","-le","-la","-les","-y","-en"])
clitiques_post_verbaux.extend(["-t-il","-t-elle","-t-on","-t-ils","-t-elles"])

# Listes des clitiques selon les fonctions syntaxiques qu'ils peuvent entretenir avec le verbe
liste_aff2sujet = ["je","j'","tu","il","elle","ils","elles","on"]
liste_aff2cod = ["le","la","les","l'"]
liste_aff2coi = ["lui","y","en"]
#cas ambigues:
liste_aff2suj_cod_coi = ["nous","vous"]
liste_aff2cod_coi = ["me","m'","te","t'","moi","toi"]

#avec tiret:
liste_aff2sujet.append(["-je","-tu","-il","-t-il","-elle","-t-elle","-ils","-t-ils","-elles","-t-elles","-on","-t-on"])
liste_aff2cod.append(["-le","-la","-les"])
liste_aff2coi.append(["-lui","-y","-en"])
#cas ambigues:
liste_aff2suj_cod_coi.append(["-nous","-vous"])
liste_aff2cod_coi.append(["-moi","-toi"])

###############
# AUXILIAIRES #
###############

# Les diff�rentes formes de "etre":
liste_etre = ['suis', 'es', 'est', 'sommes', '�tes', 'sont', '�tais', '�tait', '�tions', '�tiez', '�taient', 'serai', 'seras', 'sera', 'serons', 'serez', 'seront', 'fus', 'fut', 'f�mes', 'f�tes', 'furent', 'sois', 'soit', 'soyons', 'soyez', 'soient', 'fusse', 'fusses', 'f�t', 'fussions', 'fussiez', 'fussent', 'serais', 'serait', 'serions', 'seriez', 'seraient','�tant']

# Les formes de "etre" en fonction du mode/temps:
liste_etre_ind = ["suis","es","est","sommes","�tes","sont","�tais","�tait","�tions","�tiez","�taient"]
liste_etre_ind.extend(["�tes","�tais","�tait","�tions","�tiez","�taient"])
liste_etre_ind.extend(["fus","fut","f�mes","f�tes","furent","serai","seras","sera","serons","serez","seront"])
liste_etre_cond = ["serais","serait","serions","seriez","seraient"]
liste_etre_inf = ["�tre","�tre"]
liste_etre_ppart = ["�t�","�t�"]
liste_etre_ppres = ["�tant","�tant"]
liste_etre_subj = ["sois","soit","soyons","soyez","soient","fusse","fusses","f�t","fussions","fussiez","fussent"]
liste_etre_imp = ["sois","soyons","soyez"]

# Les formes de "etre" utilis�es pour construire le passif
liste_aux_pass = ['�t�', '�tre']

# Toutes les formes de "avoir":
liste_avoir = ['ai', 'as', 'a', 'avons', 'avez', 'ont', 'avais', 'avait', 'avions', 'aviez', 'avaient','aurai', 'auras', 'aura', 'aurons', 'aurez', 'auront', 'eus', 'eut', 'e�mes', 'e�tes', 'eurent', 'aurais', 'aurait', 'aurions', 'auriez', 'auriont' ,'aie', 'aies', 'ait', 'ayons', 'ayez', 'aient', 'eusse', 'eusses', 'e�t', 'eussions', 'eussiez', 'eussent','ayant']

# Toutes les formes de "faire":
liste_faire = ['fais', 'fait', 'faisons', 'faites', 'font', 'faisais', 'faisait', 'faisions', 'faisiez', 'faisaient','fis', 'fit', 'f�mes', 'f�tes', 'firent', 'ferai', 'feras', 'fera', 'ferons', 'ferez', 'feront','fasse', 'fasses', 'fassions', 'fassiez', 'fassent', 'fisse', 'fisses', 'fissions', 'fissiez', 'fissent','ferais', 'ferait', 'ferions', 'feriez', 'feraient','faisant','faire']

#######################
# VERBES ET AUX. ETRE #
#######################

liste_Ppart_auxEtre = ["all�", "all�e", "all�s", "all�es","apparu", "apparue", "apparus", "apparues","arriv�", "arriv�e", "arriv�s", "arriv�es",                  "d�c�d�", "d�c�d�e", "d�c�d�s", "d�c�d�es","demeur�", "demeur�e", "demeur�s", "demeur�es","devenu", "devenue", "devenus", "devenues","entr�", "entr�e", "entr�s", "entr�es","intervenu", "intervenue", "intervenus", "intervenues","mort", "morte", "morts", "mortes", "n�", "n�e", "n�s", "n�es","parti", "partie", "partis", "parties","parvenu", "parvenue", "parvenus", "parvenues","redevenu", "redevenue", "redevenus", "redevenues","reparti", "repartie", "repartis", "reparties","rest�", "rest�e", "rest�s", "rest�es","retomb�", "retomb�e", "retomb�s", "retomb�es","revenu", "revenue",  "revenus",  "revenues","tomb�", "tomb�e", "tomb�s", "tomb�es","venu", "venue",  "venus",  "venues"]

###############################
# VERBES ET ATTRIBUT DU SUJET #
###############################

copula_wordset = set(['parais','paraissaient','paraissais','paraissait','paraissant','paraisse','paraissent','paraisses','paraissez','paraissiez','paraissions','paraissons','para�t','para�tra','para�trai','para�traient','para�trais','para�trait','para�tras','para�tre','para�trez','para�triez','para�trions','para�trons','para�tront','paru','parue','parues','parurent','parus','parusse','parussent','parusses','parussiez','parussions','parut','par�mes','par�t','par�tes','apparais','apparaissaient','apparaissais','apparaissait','apparaissant','apparaisse','apparaissent','apparaisses','apparaissez','apparaissiez','apparaissions','apparaissons','appara�t','appara�tra','appara�trai','appara�traient','appara�trais','appara�trait','appara�tras','appara�tre','appara�trez','appara�triez','appara�trions','appara�trons','appara�tront','apparu','apparue','apparues','apparurent','apparus','apparusse','apparussent','apparusses','apparussiez','apparussions','apparut','appar�mes','appar�t','appar�tes','es','est','furent','fus','fusse','fussent','fusses','fussiez','fussions','fut','f�mes','f�t','f�tes','sera','serai','seraient','serais','serait','seras','serez','seriez','serions','serons','seront','soient','sois','soit','sommes','sont','soyez','soyons','suis','�taient','�tais','�tait','�tant','�tiez','�tions','�t�','�tes','�tre','resta','restai','restaient','restais','restait','restant','restas','restasse','restassent','restasses','restassiez','restassions','reste','restent','rester','restera','resterai','resteraient','resterais','resterait','resteras','resterez','resteriez','resterions','resterons','resteront','restes','restez','restiez','restions','restons','rest�mes','rest�t','rest�tes','rest�rent','rest�','rest�e','rest�es','rest�s','demeura','demeurai','demeuraient','demeurais','demeurait','demeurant','demeuras','demeurasse','demeurassent','demeurasses','demeurassiez','demeurassions','demeure','demeurent','demeurer','demeurera','demeurerai','demeureraient','demeurerais','demeurerait','demeureras','demeurerez','demeureriez','demeurerions','demeurerons','demeureront','demeures','demeurez','demeuriez','demeurions','demeurons','demeur�mes','demeur�t','demeur�tes','demeur�rent','demeur�','demeur�e','demeur�es','demeur�s','sembla','semblai','semblaient','semblais','semblait','semblant','semblas','semblasse','semblassent','semblasses','semblassiez','semblassions','semble','semblent','sembler','semblera','semblerai','sembleraient','semblerais','semblerait','sembleras','semblerez','sembleriez','semblerions','semblerons','sembleront','sembles','semblez','sembliez','semblions','semblons','sembl�mes','sembl�t','sembl�tes','sembl�rent','sembl�','sembl�e','sembl�es','sembl�s','devenaient','devenais','devenait','devenant','devenez','deveniez','devenions','devenir','devenons','devenu','devenue','devenues','devenus','deviendra','deviendrai','deviendraient','deviendrais','deviendrait','deviendras','deviendrez','deviendriez','deviendrions','deviendrons','deviendront','devienne','deviennent','deviennes','deviens','devient','devinrent','devins','devinsse','devinssent','devinsses','devinssiez','devinssions','devint','dev�nmes','dev�nt','dev�ntes','redevenaient','redevenais','redevenait','redevenant','redevenez','redeveniez','redevenions','redevenir','redevenons','redevenu','redevenue','redevenues','redevenus','redeviendra','redeviendrai','redeviendraient','redeviendrais','redeviendrait','redeviendras','redeviendrez','redeviendriez','redeviendrions','redeviendrons','redeviendront','redevienne','redeviennent','redeviennes','redeviens','redevient','redevinrent','redevins','redevinsse','redevinssent','redevinsses','redevinssiez','redevinssions','redevint','redev�nmes','redev�nt','redev�ntes'])

#########
# PIVOT #
#########

liste_relations_PIVOT = ['suj','obj','a_obj','aff','ats','ato','de_obj','mod','obj','p_obj','coord','dep_coord','aux_tps','aux_pass','aux_caus','det','ponct']
liste_relations_arg_pred_PIVOT = ['suj','obj','a_obj','aff','ats','ato','de_obj','mod','obj','p_obj']


########
# EASY #
########

liste_chunks_EASY = ['GP','GN','PV','NV','GA','GR']
liste_relations_EASY = ["suj_v","aux_v","cod_v","cpl_v","mod_v","mod_n","mod_a","mod_r","mod_p","ats","ato","comp","coord1","coord2","juxt","app"]

