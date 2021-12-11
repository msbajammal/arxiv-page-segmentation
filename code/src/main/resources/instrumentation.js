window.__CORTEXJS = {

    getROIs(root = document.body, parentXPath = "", nestedIFrames=false) {
        document.body.scrollIntoView()
        var ROIs = []
        findROIs(root, parentXPath)

        function findROIs(rootNode, xpathOfParent, parentRect=null) {
            if (parentRect==null) {
                parentRect = { top: 0, left: 0 }
            }
            var inIFrame = (xpathOfParent!="")
            var allElements = Array.from(rootNode.querySelectorAll("*"))
                                                 .filter(element => __CORTEXJS._isVisible(element, inIFrame, parentRect))

            // overlay images first
            allElements.forEach(element => {
                if (__CORTEXJS._containsImage(element)) {
                    ROIs.push(__CORTEXJS._getROIData(element, "image", xpathOfParent, parentRect))
                }
            })

            __CORTEXJS.getTextROIs(rootNode, xpathOfParent, parentRect).forEach(ROI => ROIs.push(ROI))

            // then overlay input ROIs
            allElements.forEach(element => {
                if (__CORTEXJS._containsInput(element)) {
                    ROIs.push(__CORTEXJS._getROIData(element, "input", xpathOfParent, parentRect))
                }
            })

            if (nestedIFrames) {
                Array.from(rootNode.getElementsByTagName('iframe')).forEach(iframe => {
                    if (iframe.contentDocument!=null) {
                        const relativeRect = iframe.getBoundingClientRect()
                        const absoluteRect = {
                            left: relativeRect.left   + parentRect.left,
                            right: relativeRect.right + parentRect.left,
                            top: relativeRect.top       + parentRect.top,
                            bottom: relativeRect.bottom + parentRect.top,
                            width: relativeRect.width,
                            height: relativeRect.height
                        }
                        var absoluteXPath = xpathOfParent + __CORTEXJS._getXpathOfElement(iframe)
                        iframe.absoluteXPath = absoluteXPath
                        findROIs(iframe.contentDocument.body, absoluteXPath, absoluteRect)
                    }
                })
            }
        }

//        function removeDuplicates(myArr, prop) {
//            return myArr.filter((obj, pos, arr) => {
//                return arr.map(mapObj => mapObj[prop]).indexOf(obj[prop]) === pos;
//            });
//        }

//        return removeDuplicates(ROIs, 'xpath')
        console.log(`Found a total of ${ROIs.length} ROIs.`)
        return ROIs
    },

    getTextROIs(rootNode, xpathOfParent="", parentRect=null) {
//        var xpath = '//*[not(self::script) and not(self::noscript) and not(self::style)]/text()'
//        var textNodes = document.evaluate(xpath, rootNode, null, XPathResult.ANY_TYPE, null)
//        var textROIs = [], nextNode
//        while ( (nextNode = textNodes.iterateNext())!=null ) {
//            if ( nextNode.nodeValue.replace(/(&[^;]+;)/gi,"").replace(/\W+/gi,"").length>0
//                 && __CORTEXJS._isVisible(nextNode.parentElement, parentRect) ) {
//        	    textROIs.push(__CORTEXJS._getROIData(nextNode, "text", xpathOfParent, parentRect))
//            }
//        return textROIs
    var textROIs = [];
    Array.from(document.body.querySelectorAll("*"))
        .map(node => node.firstChild&&node.firstChild.nodeName==="#text"? node : null)
        .filter(node=>node&&!['SCRIPT', 'STYLE', 'FORM'].includes(node.nodeName)).forEach(node => {
            if ( node.firstChild.nodeValue.replace(/(&[^;]+;)/gi,"").replace(/\W+/gi,"").length>0
                 && __CORTEXJS._isVisible(node, parentRect) ) {
        	    textROIs.push(__CORTEXJS._getROIData(node.firstChild, "text", xpathOfParent, parentRect))
            }
        });

        return textROIs;
    },



    _getMBRRect(node, parentRect=null) {
        var finalRect
        if (parentRect==null) {
            parentRect = {left: 0, top: 0}
        }
        if (node.nodeName=="#text") {
            var range = document.createRange()
            range.selectNode(node)
            var rects = Array.from(range.getClientRects())
            var rangeRect = range.getBoundingClientRect()
            rects.push(rangeRect)

            Array.from(node.parentElement.getClientRects()).forEach(rect => rects.push(rect))
            rects.push(node.parentElement.getBoundingClientRect())

//            console.log("====== MBR Calculations ======")
//            console.log("Text: " + node.textContent)
//            rects.forEach(rect => console.log(rect))

            rects = rects.filter(rect => rect.width*rect.height>=100)

            var rectDims = []
            for (var i=0; i<rects.length; i++) {
                rectDims.push({index: i, size: rects[i].width*rects[i].height})
            }
            rectDims.sort((a, b) => {
                return (a.size - b.size)
            })
//            console.log(" MBR is: ")
//            console.log(rects[rectDims[0].index])
            range.detach()
    //        return rects[rectDims[0].index]

            finalRect = rangeRect
        } else {
            var rects = Array.from(node.getClientRects())
            rects.push(node.getBoundingClientRect())
            rects = rects.filter(rect => rect.width*rect.height>=100)

            var rectDims = []
            for (var i=0; i<rects.length; i++) {
                rectDims.push({index: i, size: rects[i].width*rects[i].height})
            }
            rectDims.sort((a, b) => {
                return (a.size - b.size)
            })

//            finalRect = rects[rectDims[0].index]
            finalRect = node.getBoundingClientRect()
        }

        finalRect.x = Math.round(finalRect.x + parentRect.left)
        finalRect.left = Math.round(finalRect.left + parentRect.left)
        finalRect.right = Math.round(finalRect.right + parentRect.left)
        finalRect.y = Math.round(finalRect.y + parentRect.top)
        finalRect.top = Math.round(finalRect.top + parentRect.top)
        finalRect.bottom = Math.round(finalRect.bottom + parentRect.top)
        finalRect.width = Math.round(finalRect.width)
        finalRect.height = Math.round(finalRect.height)
        return finalRect
    },

    _getROIData(ROINode, ROIType, xpathOfParent="", parentRect=null) {
        const rect = __CORTEXJS._getMBRRect(ROINode, parentRect)
        var ROIElement
        if (ROINode.nodeName=="#text") {
            ROIElement = ROINode.parentElement
        } else {
            ROIElement = ROINode
        }
        var ROIData = {
            x1: rect.left, x2: rect.right,
            y1: rect.top,  y2: rect.bottom,
            width: rect.width, height: rect.height,
            xpath: xpathOfParent + __CORTEXJS._getXpathOfElement(ROIElement)
        }
        if (ROIType == "text") {
            ROIData.type = "text"
            ROIData.content = ROINode.textContent
        } else if (ROIType == "image") {
            ROIData.type = "image"
        } else if (ROIType == "input") {
            ROIData.type = "input"
        } else {
            throw new Error("unrecognized ROI type '"+ROIType+"'.")
        }

        var style = getComputedStyle(ROIElement)
        ROIData.fontSize = parseInt(style.fontSize)
        ROIData.foregroundColor = __CORTEXJS._getRGBAVector(style.color)
        ROIData.backgroundColor = __CORTEXJS._getRGBAVector(style.backgroundColor)
//        ROIData.hash = this._hashCode(ROIData)
//        ROIData.element = ROIElement // Remove field 'element' before connecting to Selenium

        return ROIData
    },

    showROIImage() {
        const ROIs = this.getROIs()
//        const ROIs = this.getAllROIs()
        this._addBackgroundOverlay()
        this._addROIsOverlay(ROIs)
    },

    removeROIImage() {
        this._removeOverlays()
    },

    _addROIsOverlay(ROIs) {
        const backgroundOverlay = document.querySelector("#__cortex_background_overlay__")
        ROIs.forEach(ROI => {
            var content = ROI.content ? ROI.content : ""
            var ROIElement = document.createElement("div")
            var backgroundColor = {
                text: "green", image: "blue", input: "red"
            }[ROI.type]
            ROIElement.style.cssText = "\
            position: absolute; top: "+ROI.y1+"px; left: "+ROI.x1+"px; \
            width: "+ROI.width+"px; height: "+ROI.height+"px; \
            background-color: "+backgroundColor+"; border: 1px solid black;"
            ROIElement.setAttribute('title', content+"\n"+ROI.xpath)
            // Remove the following event listener before connecting to Selenium
            ROIElement.addEventListener("click", () => {
                console.log(ROI.element)
            })
            backgroundOverlay.appendChild(ROIElement)
        })
    },

    _addBackgroundOverlay() {
        if (!document.querySelector("#__cortex_background_overlay__")) {
            const height = document.documentElement.scrollHeight
            const width = document.documentElement.scrollWidth
            const overlay = document.createElement("div")
            overlay.id = "__cortex_background_overlay__"
            overlay.style.cssText = "\
            z-index: 1000000; \
            background-color: black; \
            position: absolute; top: 0px; left: 0px; \
            width: "+width+"px; height: "+height+"px;"
            document.body.appendChild(overlay)
        }
    },

    _removeOverlays() {
        document.body.removeChild(
            document.querySelector("#__cortex_background_overlay__")
        )
    },

    _isVisible(element, inIFrame=false, parentRect) {
//        if (element.nodeName=="SCRIPT" || element.nodeName=="NOSCRIPT") return false;
//        const style = getComputedStyle(element);
//        const rect = element.getBoundingClientRect()
//        if (style.display === 'none') return false;
//        if (style.visibility !== 'visible') return false;
//        if (parseInt(style.width)<=1 || parseInt(style.height)<=1) return false;
//        if (style.opacity < 0.1) return false;
//        if (element.offsetWidth+element.offsetHeight+rect.height+rect.width === 0) return false;
//        if (parentRect==null || parentRect==undefined) {
//            parentRect = {left: 0, top: 0}
//        }
//        const elementCenter = {x: parentRect.left + rect.left + (rect.width / 2),
//                               y: parentRect.top + rect.top + (rect.height / 2)};
//        // Uncomment the following 'elementsFromPoint' line before using with Selenium
////        if (document.elementsFromPoint(elementCenter.x, elementCenter.y).indexOf(element)<0) return false;
//        if (!inIFrame) {
//            if (elementCenter.x < 0) return false;
//            if (elementCenter.x > document.documentElement.scrollWidth) return false;
//            if (elementCenter.y < 0) return false;
//            if (elementCenter.y > document.documentElement.scrollHeight) return false;
//        }
//        return true;
        if (element.nodeName=="SCRIPT" || element.nodeName=="NOSCRIPT" || element.nodeName=="STYLE") return false;
        const style = getComputedStyle(element);
        const rect = element.getBoundingClientRect()
        if (style.display === 'none') return false;
        if (style.visibility !== 'visible') return false;
        if (parseInt(style.width)<=1 || parseInt(style.height)<=1) return false;
        if (style.opacity < 0.1) return false;
        if (element.offsetWidth+element.offsetHeight+rect.height+rect.width === 0) return false;
        return true;
    },

    _getXpathOfElement(element) {
        var paths = [];
        for (; element && element.nodeType == 1; element = element.parentNode) {
            var index = 0;
            for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {
                if (sibling.nodeType == Node.DOCUMENT_TYPE_NODE)
                    continue;

                if (sibling.nodeName == element.nodeName)
                    ++index;
            }
            var tagName = element.nodeName.toLowerCase();
          var pathIndex = "[" + (index+1) + "]";
          paths.splice(0, 0, tagName + pathIndex);
        }
        return paths.length ? "/" + paths.join("/") : null;
    },

    _getElementFromXpath(xpath) {
        return document.evaluate(xpath, document, null, XPathResult.ANY_UNORDERED_NODE_TYPE, null).singleNodeValue
    },

    _isNonEmptyString(str) {
        const newStr = str.toString();
        return newStr.replace(/\s/g,'').replace(/&nbsp;/g,'') != '';
    },

    _containsText(element) {
        var childNodes = element.childNodes;
        for (var n=0; n<childNodes.length; n++) {
            if (childNodes[n].nodeType == 3 &&
                childNodes[n].nodeName === "#text" &&
                this._isNonEmptyString(childNodes[n].nodeValue)) {
                    return true;
                }
        }
        return false;
    },

    _containsInput(element) {
        if (element.nodeName=="INPUT" || element.nodeName=="SELECT"
            || element.nodeName=="TEXTAREA" || element.nodeName=="BUTTON") {
            return true
        } else {
            return false
        }
    },

    _containsImage(element) {
        const rect = element.getBoundingClientRect()
        if (rect.width*rect.height>=100) {
            if (element.nodeName=="IMG" && element.src!="") {
                return true
            } else if (element.nodeName=="svg") {
                return true
            } else if (element.nodeName=="VIDEO") {
                return true
            } else if (getComputedStyle(element).backgroundImage.substring(0,9)=='url("http'
                      || getComputedStyle(element).backgroundImage.substring(0,9)=='url("data') {
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    },

    _getRGBAVector(rgba) {
    	rgba = rgba.replace(/[^\d,.]/g, '').split(',').map(val => parseFloat(val))
    	if (rgba.length==4) {
    		rgba[3] = Math.round(rgba[3]*255)
    	} else if (rgba.length==3) {
    		rgba.push(255)
    	} else {
    	    throw new Error("Unknown color format")
    	}
    	return rgba
    },

    _hashCode(object) {
        const jsonStr = JSON.stringify(object)
        var hash = 0;
        if (jsonStr.length == 0) {
            return hash;
        }
        for (var i = 0; i < jsonStr.length; i++) {
            var char = jsonStr.charCodeAt(i);
            hash = ((hash<<5)-hash)+char;
            hash = hash & hash;
        }
        return hash;
    }
}