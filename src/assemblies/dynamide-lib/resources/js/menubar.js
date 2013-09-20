// VSS Version 67 - Release October 25, 1999
//
var ToolBar_Supported = false;
var Frame_Supported   = false;
var DoInstrumentation = false;

if (navigator.userAgent.indexOf("MSIE")    != -1 &&
	navigator.userAgent.indexOf("Windows") != -1 &&
	navigator.appVersion.substring(0,1) > 3)
{
	ToolBar_Supported = true;
}

if (ToolBar_Supported)
{
	var newLineChar = String.fromCharCode(10);
	var char34 = String.fromCharCode(34);
	var LastICPMenu= "";
	var CurICPMenu = "";
	var IsMenuDropDown = true;
	var HTMLStr;
	var TBLStr;
	var x = 0;
	var y = 0;
	var x2 = 0;
	var y2 = 0;
	var x3 = 0;
	var ToolbarMinWidth;
	var ToolbarMenu;
	var ToolbarLoaded = false;
	var aDefMSColor  = new Array(3);
	var aDefICPColor = new Array(3);
	var aCurMSColor  = new Array(3);
	var aCurICPColor = new Array(3);
	var ICPFont;
	var MaxMenu = 30;
	var TotalMenu = 0;
	var arrMenuInfo = new Array(30);

	// Output style sheet and toolbar ID
	document.write("<SPAN ID='StartMenu' STYLE='display:none;'></SPAN>");

	// Build toolbar template
	TBLStr  = "<TABLE BORDER=0 CELLPADDING=0 CELLSPACING=0><TR STYLE='height:20;vertical-align:middle'><!--ICP_MENU_TITLES--></TR></TABLE>";
	HTMLStr =
		"<DIV ID='idICPMenuPane' STYLE='position:absolute;top:0;left:0;height:20;color:white;background-color:black;' NOWRAP><!--ICP_MENU_TITLES--></DIV>" +
		"</DIV>" +
		"<SCRIPT TYPE='text/javascript'>" +
		"   var ToolbarMenu = StartMenu;" +
		"</SCRIPT>" +
		"<DIV WIDTH=100%>";

	// Intialize global variables
	ICPFont = "bold xx-small Verdana";

	aDefMSColor[0]	= aCurMSColor[0]  = "black";	// bgcolor;
	aDefMSColor[1]	= aCurMSColor[1]  = "white";	// text font color
	aDefMSColor[2]  = aCurMSColor[2]  = "red";		// mouseover font color

	aDefICPColor[0]	= aCurICPColor[0] = "#6699CC";	// bgcolor;
	aDefICPColor[1] = aCurICPColor[1] = "white";	// text font color
	aDefICPColor[2] = aCurICPColor[2] = "red";		// mouseover font color
}

// correspond to the dimension of the four gif files:
//		ICP_BANNER: 60h x 250w
//		ADS_BANNER: 40h x 200w
//		MSC_BANNER: 40h x 112w
//		Curve:	    20h x 18w

function drawToolbar()
{
	HTMLStr += "</DIV>";
	document.write(HTMLStr);
	ToolbarLoaded = true;
    ToolbarMinWidth = (250+18);

	idICPMenuPane.style.backgroundColor = aDefICPColor[0];
	idICPMenuPane.style.color			= aDefICPColor[1];
	resizeToolbar();

	for (i = 0; i < TotalMenu; i++)
	{
		thisMenu = document.all(arrMenuInfo[i].IDStr);
		if (thisMenu != null)
		{
            //if (arrMenuInfo[i].IDStr == LastMSMenu && arrMenuInfo[i].type == "R")
            //{
				//Last MSMenu has to be absolute width
                //arrMenuInfo[i].type = "A";
                //arrMenuInfo[i].unit = 200;
            //}
			if (arrMenuInfo[i].type == "A")
				thisMenu.style.width = arrMenuInfo[i].unit;
			else
				thisMenu.style.width = Math.round(arrMenuInfo[i].width * arrMenuInfo[i].unit) + 'em';
		}
	}
}

function resizeToolbar()
{
	if (ToolBar_Supported == false) return;
	w = Math.max(ToolbarMinWidth, document.body.clientWidth) - ToolbarMinWidth;
	idICPMenuPane.style.width = ToolbarMinWidth + w;
}

function setICPMenuFont(sFont)
{	ICPFont = sFont;
}

function setDefaultICPMenuColor(bgColor, fontColor, mouseoverColor)
{
	if (bgColor   != "")	  aDefICPColor[0] = bgColor;
	if (fontColor != "")	  aDefICPColor[1] = fontColor;
	if (mouseoverColor != "") aDefICPColor[2] = mouseoverColor;
}

function setICPMenuColor(MenuIDStr, bgColor, fontColor, mouseoverColor)
{
	if (ToolbarLoaded == false) return;

	// Reset previous ICP Menu color if any
	if (CurICPMenu != "")
	{
		PrevID = CurICPMenu.substring(4);
		CurICPMenu = "";
		setICPMenuColor(PrevID, aDefICPColor[0], aDefICPColor[1], aDefICPColor[2]);
	}

	var	id = "AM_" + "ICP_" + MenuIDStr;
	var thisMenu = document.all(id);
	if (thisMenu != null)
	{
		CurICPMenu = "ICP_" + MenuIDStr;
		aCurICPColor[0] = bgColor;
		aCurICPColor[1] = fontColor;
		aCurICPColor[2] = mouseoverColor;

		// Change menu color
		if (bgColor != "")
			thisMenu.style.backgroundColor = bgColor;
		if (fontColor != "")
			thisMenu.children(0).style.color = fontColor;

		// Change subMenu color
		id = "ICP_" + MenuIDStr;
		thisMenu = document.all(id);
		if (thisMenu != null)
		{
			if (bgColor != "")
				thisMenu.style.backgroundColor = bgColor;

			if (fontColor != "")
			{
				i = 0;
				id = "AS_" + "ICP_" + MenuIDStr;
				thisMenu = document.all.item(id,i);
				while (thisMenu != null)
				{
					thisMenu.style.color = fontColor;
					i += 1;
					thisMenu = document.all.item(id,i);
				}
			}
		}
	}
}

function setICPSubMenuWidth(MenuIDStr, WidthType, WidthUnit)
{	tempID = "ICP_" + MenuIDStr;
	setSubMenuWidth(tempID, WidthType, WidthUnit);
}

function setMSSubMenuWidth(MenuIDStr, WidthType, WidthUnit)
{	tempID = "MS_" + MenuIDStr;
	setSubMenuWidth(tempID, WidthType, WidthUnit);
}

function setSubMenuWidth(MenuIDStr, WidthType, WidthUnit)
{
	var fFound = false;
	if (TotalMenu == MaxMenu)
	{
		alert("Unable to process menu. Maximum of " + MaxMenu + " reached.");
		return;
	}

	for (i = 0; i < TotalMenu; i++)
		if (arrMenuInfo[i].IDStr == MenuIDStr)
		{
			fFound = true;
			break;
		}

	if (!fFound)
	{
		arrMenuInfo[i] = new menuInfo(MenuIDStr);
		TotalMenu += 1;
	}

	if (!fFound && WidthType.toUpperCase().indexOf("DEFAULT") != -1)
	{
		arrMenuInfo[i].type = "A";
		arrMenuInfo[i].unit = 160;
	}
	else
	{
		arrMenuInfo[i].type = (WidthType.toUpperCase().indexOf("ABSOLUTE") != -1)? "A" : "R";
		arrMenuInfo[i].unit = WidthUnit;
	}
}

// This function creates a menuInfo object instance.
function menuInfo(MenuIDStr)
{
	this.IDStr = MenuIDStr;
	this.type  = "";
	this.unit  = 0;
	this.width = 0;
	this.count = 0;
}

function updateSubMenuWidth(MenuIDStr)
{
	for (i = 0; i < TotalMenu; i++)
		if (arrMenuInfo[i].IDStr == MenuIDStr)
		{
			if (arrMenuInfo[i].width < MenuIDStr.length)
				arrMenuInfo[i].width = MenuIDStr.length;
			arrMenuInfo[i].count = arrMenuInfo[i].count + 1;
			break;
		}
}

function addICPMenu(MenuIDStr, MenuDisplayStr, MenuHelpStr, MenuURLStr)
{
	if (LastICPMenu == "") HTMLStr = HTMLStr.replace("<!--ICP_MENU_TITLES-->", TBLStr);

	if (addICPMenu.arguments.length > 4)
		TargetStr = addICPMenu.arguments[4];
	else
		TargetStr = "_top";
	tempID = "ICP_" + MenuIDStr;
	addMenu(tempID, MenuDisplayStr, MenuHelpStr, MenuURLStr, TargetStr, true);
	LastICPMenu = tempID;
}

function addMenu(MenuIDStr, MenuDisplayStr, MenuHelpStr, MenuURLStr, TargetStr, bICPMenu)
{
    cFont   = ICPFont;
	cColor0 = bICPMenu? aDefICPColor[0] : aDefMSColor[0];
	cColor1 = bICPMenu? aDefICPColor[1] : aDefMSColor[1];
	cColor2 = bICPMenu? aDefICPColor[2] : aDefMSColor[2];
	cStyle  = "font:" + cFont + ";background-color:" + cColor0 + ";color:" + cColor1 + ";";
	tagStr  = bICPMenu? "<!--ICP_MENU_TITLES-->" : "<!--MS_MENU_TITLES-->";

	if (MenuHelpStr == "") MenuHelpStr = MenuDisplayStr;
	MenuStr = newLineChar;
	if (bICPMenu)
		MenuStr += "<TD STYLE='" + cStyle + "' ID='AM_" + MenuIDStr + "' NOWRAP>";
	else
	{
		MenuStr += "<SPAN STYLE='" + cStyle + "'>";
		MenuStr += "&nbsp;";
	}

	MenuStr += "<A STYLE='text-decoration:none;cursor:hand;font:" + cFont + ";color:" + cColor1 + ";'" +
			   "   TARGET='" + TargetStr + "'" +
			   "   TITLE=" + char34 + MenuHelpStr + char34;
	if (MenuURLStr != "")
		MenuStr += " HREF='" + formatURL(MenuURLStr, ((bICPMenu? "ICP_":"MS_") + MenuDisplayStr)) + "'";
	else
		MenuStr += " HREF='' onclick='window.event.returnValue=false;'";
	MenuStr += " onmouseout="  + char34 + "mouseMenu('out' ,'" + MenuIDStr + "'); hideMenu();" + char34 +
			   " onmouseover=" + char34 + "mouseMenu('over','" + MenuIDStr + "'); doMenu('"+ MenuIDStr + "');" + char34 + ">" +
			   "&nbsp;" + MenuDisplayStr + "&nbsp;</a>";
	if (bICPMenu)
		MenuStr += "&nbsp;</TD><TD STYLE='" + cStyle + "'>|</TD>";
	else
		MenuStr += "</SPAN>";

	MenuStr += tagStr;
	HTMLStr = HTMLStr.replace(tagStr, MenuStr);
	setSubMenuWidth(MenuIDStr,"default",0);
}

function addICPSubMenu(MenuIDStr, SubMenuStr, SubMenuURLStr)
{
	if (addICPSubMenu.arguments.length > 3)
		TargetStr = addICPSubMenu.arguments[3];
	else
		TargetStr = "_top";
	tempID = "ICP_" + MenuIDStr;
	addSubMenu(tempID,SubMenuStr,SubMenuURLStr,TargetStr,true);
}

function addMSSubMenu(MenuIDStr, SubMenuStr, SubMenuURLStr)
{
	TargetStr = "_top";
	tempID = "MS_" + MenuIDStr;
	addSubMenu(tempID,SubMenuStr,SubMenuURLStr,TargetStr,false);
}

function addSubMenu(MenuIDStr, SubMenuStr, SubMenuURLStr, TargetStr, bICPMenu)
{
    cFont   =ICPFont;
	cColor0 = bICPMenu? aDefICPColor[0] : aDefMSColor[0];
	cColor1 = bICPMenu? aDefICPColor[1] : aDefMSColor[1];
	cColor2 = bICPMenu? aDefICPColor[2] : aDefMSColor[2];

	var MenuPos = MenuIDStr.toUpperCase().indexOf("MENU");
	if (MenuPos == -1) { MenuPos = MenuIDStr.length; }
	InstrumentStr = MenuIDStr.substring(0 , MenuPos) + "|" + SubMenuStr;;
	URLStr        = formatURL(SubMenuURLStr, InstrumentStr);

	var LookUpTag  = "<!--" + MenuIDStr + "-->";
	var sPos = HTMLStr.indexOf(LookUpTag);
	if (sPos <= 0)
	{
		HTMLStr += newLineChar + newLineChar +
				"<SPAN ID='" + MenuIDStr + "'" +
				" STYLE='display:none;position:absolute;width:160;background-color:" + cColor0 + ";padding-top:0;padding-left:0;padding-bottom:20;z-index:9;'" +
				" onmouseout='hideMenu();'>";
		if (Frame_Supported == false || bICPMenu == false)
			HTMLStr += "<HR  STYLE='position:absolute;left:0;top:0;color:" + cColor1 + "' SIZE=1>";
		HTMLStr += "<DIV STYLE='position:relative;left:0;top:8;'>";
	}

	TempStr = newLineChar +
				"<A ID='AS_" + MenuIDStr + "'" +
				"   STYLE='text-decoration:none;cursor:hand;font:" + cFont + ";color:" + cColor1 + "'" +
				"   HREF='" + URLStr + "' TARGET='" + TargetStr + "'" +
				" onmouseout="  + char34 + "mouseMenu('out' ,'" + MenuIDStr + "');" + char34 +
				" onmouseover=" + char34 + "mouseMenu('over','" + MenuIDStr + "');" + char34 + ">" +
				"&nbsp;" + SubMenuStr + "</A><BR>" + LookUpTag;
	if (sPos <= 0)
		HTMLStr += TempStr + "</DIV></SPAN>";
	else
		HTMLStr = HTMLStr.replace(LookUpTag, TempStr);

	updateSubMenuWidth(MenuIDStr);
}

function addICPSubMenuLine(MenuIDStr)
{
	tempID = "ICP_" + MenuIDStr;
	addSubMenuLine(tempID,true);
}

function addMSSubMenuLine(MenuIDStr)
{
	tempID = "MS_" + MenuIDStr;
	addSubMenuLine(tempID,false);
}

function addSubMenuLine(MenuIDStr, bICPMenu)
{
	var LookUpTag = "<!--" + MenuIDStr + "-->";
	var sPos = HTMLStr.indexOf(LookUpTag);
	if (sPos > 0)
	{
		cColor  = bICPMenu? aDefICPColor[1] : aDefMSColor[1];
		TempStr = newLineChar + "<HR STYLE='color:" + cColor + "' SIZE=1>" + LookUpTag;
		HTMLStr = HTMLStr.replace(LookUpTag, TempStr);
	}
}

function mouseMenu(id, MenuIDStr)
{
	IsMouseout = (id.toUpperCase().indexOf("OUT") != -1);

	if (IsMouseout)
	{
        color =  aDefICPColor[1];
		if (MenuIDStr == CurICPMenu && aCurICPColor[1] != "")
			color = aCurICPColor[1];
	}
	else
	{
        color =  aDefICPColor[2];
		if (MenuIDStr == CurICPMenu && aCurICPColor[2] != "")
			color = aCurICPColor[2];
	}
	window.event.srcElement.style.color = color;
}

function doMenu(MenuIDStr)
{
	var thisMenu = document.all(MenuIDStr);
	if (ToolbarMenu == null || thisMenu == null || thisMenu == ToolbarMenu)
	{
		window.event.cancelBubble = true;
		return false;
	}
	// Reset dropdown menu
	window.event.cancelBubble = true;
	ToolbarMenu.style.display = "none";
	showElement("SELECT");
	showElement("OBJECT");
	showElement("IFRAME");
	ToolbarMenu = thisMenu;

	// Set dropdown menu display position
	x  = window.event.srcElement.offsetLeft +
	 	 window.event.srcElement.offsetParent.offsetLeft;
    y = idICPMenuPane.offsetHeight;//laramie

	// Get main menu width
    x2 = x + (window.event.srcElement.offsetWidth);
	// Get dropdown menu width
	x3 = x + 160;
	for (i = 0; i < TotalMenu; i++)
		if (arrMenuInfo[i].IDStr == MenuIDStr)
		{
			x3 = x+ arrMenuInfo[i].unit;
			break;
		}
	thisMenu.style.top  = y;
	thisMenu.style.left = x;
	thisMenu.style.clip = "rect(0 0 0 0)";
	thisMenu.style.display = "block";

	// delay 2 millsecond to allow the value of ToolbarMenu.offsetHeight be set
	window.setTimeout("showMenu()", 2);
	return true;
}

function showMenu()
{
	if (ToolbarMenu != null)
	{
        IsMenuDropDown = (Frame_Supported)? false : true;    //%%
		if (IsMenuDropDown == false)
		{
            y = (y - ToolbarMenu.offsetHeight);
			if (y < 0) y = 0;
			ToolbarMenu.style.top = y;
		}
		// Get dropdown menu height
		y2 = y + ToolbarMenu.offsetHeight;

		ToolbarMenu.style.clip = "rect(auto auto auto auto)";
		hideElement("SELECT");
		hideElement("OBJECT");
		hideElement("IFRAME");
	}
}

function hideMenu()
{
	if (ToolbarMenu != null && ToolbarMenu != StartMenu)
	{
		var cX = event.clientX + document.body.scrollLeft;
		var cY = event.clientY + document.body.scrollTop;
		var bHideMenu = true;

		if (IsMenuDropDown == true)
		{
            if (cY >= (y - 10) && cY < y)
			{
				if (cX >= (x+5) && cX <= x2) bHideMenu = false;
			}
			else if (cY >= y && cY <= y2)
			{
				if (cX > (x+5) && cX <= x3) bHideMenu = false;
			}
		}
		else
		{
            if (cY >= y2 && cY < (y2))
			{
				if (cX >= (x+5) && cX <= x2) bHideMenu = false;
			}
			else if (cY >= y && cY <= y2)
			{
				if (cX > (x+5) && cX <= x3) bHideMenu = false;
			}
		}

		if (! bHideMenu)
		{
			window.event.cancelBubble = true;
			return;
		}

		ToolbarMenu.style.display = "none";
		ToolbarMenu = StartMenu;
		window.event.cancelBubble = true;

		showElement("SELECT");
		showElement("OBJECT");
		showElement("IFRAME");
	}
}

function hideElement(elmID)
{
	for (i = 0; i < document.all.tags(elmID).length; i++)
	{
		obj = document.all.tags(elmID)[i];
		if (! obj || ! obj.offsetParent)
			continue;

		// Find the element's offsetTop and offsetLeft relative to the BODY tag.
		objLeft   = obj.offsetLeft;
		objTop    = obj.offsetTop;
		objParent = obj.offsetParent;
		while (objParent.tagName.toUpperCase() != "BODY")
		{
			objLeft  += objParent.offsetLeft;
			objTop   += objParent.offsetTop;
			objParent = objParent.offsetParent;
		}
		// Adjust the element's offsetTop relative to the dropdown menu
		objTop = objTop - y;

		if (x > (objLeft + obj.offsetWidth) || objLeft > (x + ToolbarMenu.offsetWidth))
			;
		else if (objTop > ToolbarMenu.offsetHeight)
			;
		else
			obj.style.visibility = "hidden";
	}
}

function showElement(elmID)
{
	for (i = 0; i < document.all.tags(elmID).length; i++)
	{
		obj = document.all.tags(elmID)[i];
		if (! obj || ! obj.offsetParent)
			continue;
		obj.style.visibility = "";
	}
}

function formatURL(URLStr, InstrumentStr)
{
	var tempStr = URLStr;

	if (DoInstrumentation && URLStr != "" )
	{
		var ParamPos1 = URLStr.indexOf("?");
		var ParamPos2 = URLStr.lastIndexOf("?");
		var ParamPos3 = URLStr.toLowerCase().indexOf("target=");
		var ParamPos4 = URLStr.indexOf("#");
		var Bookmark  = "";
		var URL = URLStr;
		if (ParamPos4 >= 0)
		{
		 	URL = URLStr.substr(0, ParamPos4);
			Bookmark = URLStr.substr(ParamPos4);
		}

		if (ParamPos1 == -1)
			tempStr = "?MSCOMTB=";
		else if (ParamPos1 == ParamPos2 && ParamPos3 == -1)
			tempStr = "&MSCOMTB=";
		else if (ParamPos1 == ParamPos2 && ParamPos3 != -1)
			tempStr = "?MSCOMTB=";
		else if (ParamPos1 < ParamPos2)
			tempStr = "&MSCOMTB=";

		tempStr = URL + tempStr + InstrumentStr.replace(" ","%20") + Bookmark;
	}
	return tempStr;
}