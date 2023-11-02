import React, {useState} from "react";
import "./index.css";
import VerifyCertificateImg from "../../assets/img/verify-certificate.png"
import QRCodeImg from "../../assets/img/qr-code.svg"
import {CertificateStatus} from "../CertificateStatus";
import {CustomButton} from "../CustomButton";
import QRScanner from "../QRScanner";
import JSZip from "jszip";

export const CERTIFICATE_FILE = "certificate.json";

export const VerifyCertificate = () => {
    // language=JSON
    const [result, setResult] = useState("");
    const [showScanner, setShowScanner] = useState(false);
    const handleScan = async (data) => {

        console.log("data from scan after decode", data);
        if (data) {
            const zip = new JSZip();
            await zip.loadAsync(data).then((contents) => {
                console.log("content---->",contents);
                console.log(contents.files[CERTIFICATE_FILE].async("text"))
                return contents.files[CERTIFICATE_FILE].async("text")
            }).then(function (contents) {
                debugger
                console.log("result set from content which is passed to certificate status--->", contents);
                setResult(contents)
            }).catch(err => {
                console.log("error while decoding _----> ", err)
                    setResult(data)
                }
            );

        }
    };
    const handleError = err => {
        console.error(err)
    };
    return (
        <div className="container-fluid verify-certificate-wrapper">
            {
                !result &&
                <>
                    {!showScanner &&
                        <>
                            <img src={VerifyCertificateImg} className="banner-img" alt="banner-img"/>
                            <h3 className="text-center">Verify Sunbird RC Certificate</h3>
                            <CustomButton className="green-btn" onClick={() => setShowScanner(true)}>
                                <span>Scan QR code</span>
                                <img className="ml-3" src={QRCodeImg} alt={""}/>
                            </CustomButton>
                        </>}
                    {showScanner &&
                        <>
                            <QRScanner onError={handleError}
                                       onScan={handleScan}/>
                            <CustomButton className="green-btn"
                                          onClick={() => setShowScanner(false)}>BACK</CustomButton>
                        </>
                    }
                </>
            }
            {
                result && <CertificateStatus certificateData={JSON.parse(result)} goBack={() => {
                    setShowScanner(false);
                    setResult("");
                }
                }/>
            }


        </div>
    )
};
