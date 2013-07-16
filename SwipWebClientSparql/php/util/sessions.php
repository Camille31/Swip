<?php
// Initialisation des variables de sessions.
session_start();

// Import des fonctions permettant de gerer la base de donn�e relationnelle
include "dataBases.php";

/******************************************
 * Connexions aux pages d'administration. *
 ******************************************/
function initConnections() {
	/*
	 * D�finition de la variable globale $isConnected permettant de v�rifier si
	 * l'utilisateur est connect� ou non, et la variable globale $formSubmitted
	 * permettant de savoir si l'utilisateur est arriv� sur cette page apr�s
	 * avoir soumit un formulaire de connexion ou non
	 */
	global $isConnected, $formSubmitted;
	
	$formSubmitted = false;
	
	// Si l'utilisateur arrive sur la page en ayant rempli un formulaire de connexion.
	if (isset($_POST['id']) && isset($_POST['mdp'])) {
		$formSubmitted = true;
		
		// V�rification de la validit� de l'identifiant et du mot de passe.
		if (verifAcces($_POST['id'], $_POST['mdp'])) {
			$isConnected = true;
			$_SESSION['id'] = $_POST['id'];
			$_SESSION['mdp'] = $_POST['mdp'];
		} else {
			$isConnected = false;
		}
	} elseif (isset($_SESSION['id']) && isset($_SESSION['mdp'])) { // sinon s'il s'est d�j� connect�
		if (verifAcces($_SESSION['id'], $_SESSION['mdp'])) { // et que l'identifiant et le mot de passe restent bons
			$isConnected = true;
		} else {
			$isConnected = false;
		}
	} else {
		$isConnected = false;
	}
}

/*
 * v�rifie si $id et $mdp correspondent bien � l'identifiant et le mot de passe
 * d'un administrateur (enregistr� dans la base de donn�es de swip).
 */
function verifAcces($id, $mdp) {
	/*
	 * D�finition des variables globales $idIsValid et $mdpIsValid
	 * permettant de v�rifier que l'identifiant et le mot de passe entr�s
	 * sont corrects.
	 */
	global $idIsValid, $mdpIsValid;
	$idIsValid = $mdpIsValid = false;
	
	global $currentID, $currentMdp;
	$currentID = $id;
	$currentMdp = $mdp;
	
	// connexion � la base de donn�es de swip
	dbConnect();
	
	// liste de tous les administrateurs et comparaisons avec l'identifiant et le mot de passe donn�s
	$sqlQuery = "select * from administrators;";
	
	processSqlQuery($sqlQuery, function ($resp) {
		global $idIsValid, $mdpIsValid, $currentID, $currentMdp;
		
		// pour chaque entr�e trouv�e
		while ($data = mysql_fetch_assoc($resp)) {
			if ($currentID == $data['id']) {
				$idIsValid = true;
				
				if ($currentMdp == $data['mdp'])
					$mdpIsValid = true;
			}
		}
	});
	
	return $idIsValid && $mdpIsValid;
}
?>