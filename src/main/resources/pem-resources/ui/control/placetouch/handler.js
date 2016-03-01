var EnoceanControlUIHandler = 
{
	UID: "fr.immotronic.ubikit.pems.enocean",
	baseURL: "/Immotronic/enocean/",
	
	init: function()
	{
		Placetouch.registerPemControlUIHandler(EnoceanControlUIHandler.UID, EnoceanControlUIHandler);
		EnoceanControlUIHandler.__loadCapabilityUIs();
	},
		
	addControlUI: function(itemUID, capability, configuration, sel)
	{
		var ui = EnoceanControlUIHandler.__capabilityUIs[capability]; 
		if(ui != undefined)
		{
			$(sel).append(ui);
			EnoceanControlUIHandler.__initUI(itemUID, capability, configuration);
		}
	},
	
	doesControlUIExistFor: function(capability)
	{
		return EnoceanControlUIHandler.__capabilityUIs[capability] != undefined;
	},
	
	__initUI: function(itemUID, capability, configuration)
	{
		switch(capability)
		{
			case "ONOFF_DEVICE":
				$("#Enocean_onButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/on/"+itemUID, {});
				});
				
				$("#Enocean_offButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/off/"+itemUID, {});
				});
				
				break;
				
			case "BLIND_AND_SHUTTER_MOTOR_DEVICE":
				
				cwf.api.get_query(EnoceanControlUIHandler.baseURL+"api/getProperties/"+itemUID, {
					success: function(data)
					{
						if(data.inverted) {
							$("#Enocean_invertMotorCheckbox").attr("checked", "checked");
						}
						
						if(data.commandOnShortPressing) {
							$("INPUT:radio[name=Enocean_pressingTypeRadio]").removeAttr("checked");
							$("INPUT:radio[name=Enocean_pressingTypeRadio][value=true]").attr("checked", "checked");
						}
						else {
							$("INPUT:radio[name=Enocean_pressingTypeRadio]").removeAttr("checked");
							$("INPUT:radio[name=Enocean_pressingTypeRadio][value=false]").attr("checked", "checked");
						}
					}
				});
				
				$("#Enocean_upButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/up/"+itemUID, {});
				});
				
				$("#Enocean_stopButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/stop/"+itemUID, {});
				});
				
				$("#Enocean_downButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/down/"+itemUID, {});
				});
				
				$("#Enocean_send1Button").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/send1/"+itemUID, {});
				});
				
				$("#Enocean_send0Button").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/send0/"+itemUID, {});
				});
				
				$("#Enocean_invertMotorCheckbox").change(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/invertMotor/"+itemUID, { inverted: ($("#Enocean_invertMotorCheckbox").attr("checked")=="checked") } );
				});
				
				$("INPUT:radio[name=Enocean_pressingTypeRadio][value=true]").change(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setPressingType/"+itemUID, { shortPressing: $("INPUT:radio[name=Enocean_pressingTypeRadio]:checked").val() } );
				});
				
				$("INPUT:radio[name=Enocean_pressingTypeRadio][value=false]").change(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setPressingType/"+itemUID, { shortPressing: $("INPUT:radio[name=Enocean_pressingTypeRadio]:checked").val() } );
				});
				
				break;
				
			case "RH_DEVICE":
				$("#Enocean_levelRange").change(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/outputLevel/"+itemUID, { level: $("#Enocean_levelRange").val() });
				});
				
				break;
				
			case "INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE":
				$("#EnoceanPairDkrceno1i1icButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/pair", {
						actuatorUID: itemUID,
						nbOfSignals: 1
					});
				});
				
				$("#dkrceno1i1icMode").change(function() {
					var value = $("#dkrceno1i1icMode").val();
					if(value != "") {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setHVACMode/"+itemUID+"/"+value, {});
					}
				});
				
				$("#dkrceno1i1icFanSpeed").change(function() {
					var value = $("#dkrceno1i1icFanSpeed").val();
					if(value != "") {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setHVACFanSpeed/"+itemUID+"/"+value, {});
					}
				});
				
				$("#dkrceno1i1icVanePosition").change(function() {
					var value = $("#dkrceno1i1icVanePosition").val();
					if(value != "") {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setHVACVanePosition/"+itemUID+"/"+value, {});
					}
				});
				
				$("#dkrceno1i1icPermission").change(function() {
					var value = $("#dkrceno1i1icPermission").val();
					if(value != "") {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/IBExternalDisablement/"+itemUID+"/"+value, {});
					}
				});
				
				$("#dkrceno1i1icRemoteControllerPermission").change(function() {
					var value = $("#dkrceno1i1icRemoteControllerPermission").val();
					if(value != "") {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/IBRemoteControllerDisablement/"+itemUID+"/"+value, {});
					}
				});
				
				$("#dkrceno1i1icWindowState").change(function() {
					var value = $("#dkrceno1i1icWindowState").val();
					if(value != "") {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/IBWindowsStatus/"+itemUID+"/"+value, {});
					}
				});
				
				$("#dkrceno1i1icSetPointTemperature").change(function() {
					var value = $("#dkrceno1i1icSetPointTemperature").val();
					if(value != "") {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setHVACSetPoint/"+itemUID+"/"+value, {});
					}
				});
				
				$("#dkrceno1i1icOnButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setHVACOnOff/"+itemUID+"/ON", {});
				});
				
				$("#dkrceno1i1icOffButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/setHVACOnOff/"+itemUID+"/OFF", {});
				});
				
				break;
				
			case "EEP_D2_01_00":
			case "EEP_D2_01_02":
			case "EEP_D2_01_06":
				$("#eswemOnButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMOn/"+itemUID, {});
					if (capability == "EEP_D2_01_02")
						$("#eswemLevelRange").val(100);
				});
				
				$("#eswemOffButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMOff/"+itemUID, {});
					if (capability == "EEP_D2_01_02")
						$("#eswemLevelRange").val(0);
				});
				
				$("#eswemupdateQueryButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMUpdateQuery/"+itemUID, {});
				});
				
				if (capability == "EEP_D2_01_02")
				{					
					cwf.api.get_query(EnoceanControlUIHandler.baseURL+"api/getDimmerValue/"+itemUID, {
						success: function(data)
						{
							$("#eswemLevelRange").val(data.dimmerValue);
						}
					});
					
					$("#eswemLevelRange").change(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMDim/"+itemUID, { value: $("#eswemLevelRange").val() });
					});
				}
				
				// No pairing needed, so return.
				return;
				
			case "EEP_D2_01_11":
				if(configuration.mode == "RELAY")
				{
					$("#EEPD20111_MOTOR").hide();
					
					$("#eswemOnButton0").click(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMOn/"+itemUID+"/0", {});
					});
					
					$("#eswemOffButton0").click(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMOff/"+itemUID+"/0", {});
					});
					
					$("#eswemOnButton1").click(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMOn/"+itemUID+"/1", {});
					});
					
					$("#eswemOffButton1").click(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/ESWEMOff/"+itemUID+"/1", {});
					});
					
					$("#EEPD20111_RELAY").show();
				}
				else if(configuration.mode == "MOTOR")
				{
					$("#EEPD20111_RELAY").hide();
					
					$("#Enocean_eswem_upButton").click(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/up/"+itemUID, {});
					});
					
					$("#Enocean_eswem_stopButton").click(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/stop/"+itemUID, {});
					});
					
					$("#Enocean_eswem_downButton").click(function() {
						cwf.api.query(EnoceanControlUIHandler.baseURL+"api/down/"+itemUID, {});
					});
					
					$("#EEPD20111_MOTOR").show();
				}
				
				
				// No pairing needed, so return.
				return;
		}
		
		$("#EnoceanPairAcutator1Button").click(function() {
			cwf.api.query(EnoceanControlUIHandler.baseURL+"api/pair", {
				actuatorUID: itemUID,
				nbOfSignals: 1
			});
		});
		
		$("#EnoceanPairAcutator2Button").click(function() {
			cwf.api.query(EnoceanControlUIHandler.baseURL+"api/pair", {
				actuatorUID: itemUID,
				nbOfSignals: 2
			});
		});
		
		$("#EnoceanPairAcutator3Button").click(function() {
			cwf.api.query(EnoceanControlUIHandler.baseURL+"api/pair", {
				actuatorUID: itemUID,
				nbOfSignals: 3
			});
		});
	},
	
	__loadCapabilityUIs: function()
	{
		EnoceanControlUIHandler.__loadCapabilityUI("ONOFF_DEVICE");
		EnoceanControlUIHandler.__loadCapabilityUI("BLIND_AND_SHUTTER_MOTOR_DEVICE");
		EnoceanControlUIHandler.__loadCapabilityUI("RH_DEVICE");
		EnoceanControlUIHandler.__loadCapabilityUI("INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE");
		EnoceanControlUIHandler.__loadCapabilityUI("EEP_D2_01_00");
		EnoceanControlUIHandler.__loadCapabilityUI("EEP_D2_01_02");
		EnoceanControlUIHandler.__loadCapabilityUI("EEP_D2_01_06");
		EnoceanControlUIHandler.__loadCapabilityUI("EEP_D2_01_11");
	},
	
	__loadCapabilityUI: function(capability)
	{
		$.ajax({	
			type: "GET", 
			url:EnoceanControlUIHandler.baseURL + "ui/control/placetouch/"+capability+".html",
			success: function(data) {
				EnoceanControlUIHandler.__capabilityUIs[capability] = data;
			},
			error: function() { 
				cwf.log("No control UI for "+capability+" capability in PEM "+EnoceanControlUIHandler.UID);
			}
		});
	},
	
	__capabilityUIs: {}
}

EnoceanControlUIHandler.init();